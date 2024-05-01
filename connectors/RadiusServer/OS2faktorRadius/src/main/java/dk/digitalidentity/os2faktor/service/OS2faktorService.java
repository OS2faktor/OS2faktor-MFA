package dk.digitalidentity.os2faktor.service;

import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.os2faktor.model.Client;
import dk.digitalidentity.os2faktor.service.model.ClientSearchParams;
import dk.digitalidentity.os2faktor.service.model.SubscriptionInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OS2faktorService {
	private enum LoginStatus { ACCEPTED, REJETED, NONE };
	private RestTemplate restTemplate;

	@Autowired
	private LdapService ldapService;
	
	@Autowired
	private ClientSearchParamsService clientSearchParamsService;
	
	@Value("${login.backend.baseurl}")
	private String baseUrl;

	@Value("${login.backend.apikey}")
	private String apiKey;
	
	@Value("${login.connector.version}")
	private String connectorVersion;

	@Value("${login.connector.requirepin}")
	private boolean requirePin;
	
	@Value("${login.mfa.enabled:true}")
	private boolean mfaEnabled;

	@Value("${login.connector.preferPrimeClient:false}")
	private boolean preferPrimeClient;

	public OS2faktorService() {
		CloseableHttpClient client = HttpClients.custom()
			.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
			.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(client);

		restTemplate = new RestTemplate(requestFactory);
	}
	
	public boolean verifyPassword(String username, String password) {

		try {
			// start by verifying the username/password against Active Directory
			if (!ldapService.verifyPassword(username, password)) {
				log.warn("Wrong AD password given for " + username);
				return false;
			}
			
			log.debug("Password ok for " + username);
			
			if (!mfaEnabled) {
				log.warn("MFA validation is disabled - returning success for " + username + " without performing MFA validation");
				return true;
			}

			// then perform OS2faktor validation
			ClientSearchParams params = clientSearchParamsService.getUserDetails(username);
			log.debug("lookup parameters for " + username + ": " + params.toString());
			List<Client> clients = getClients(params);
			log.debug("Found " + (clients != null ? clients.size() : 0) + " MFA clients for " + username);
			
			// filter out yubikeys
			if (clients != null) {
				clients = clients.stream().filter(c -> !c.getType().equals("YUBIKEY")).collect(Collectors.toList());

				log.debug("Found " + clients.size() + " MFA clients for " + username + " after filtering out Yubikeys");
				
				// filter out clients that does not have a pincode (if required)
				if (requirePin) {
					clients = clients.stream().filter(c -> c.isHasPincode()).collect(Collectors.toList());
					
					log.debug("Found " + clients.size() + " MFA clients for " + username + " after filtering out clients with no pincode");
				}
			}

			if (clients != null && clients.size() > 0) {
				List<SubscriptionInfo> challenges = new ArrayList<>();

				if (preferPrimeClient) {
					Client primeClient = clients.stream().filter(c -> c.isPrime()).findFirst().orElse(null);

					if (primeClient != null) {
						SubscriptionInfo challenge = initAuthentication(primeClient.getDeviceId());
						challenges.add(challenge);

						log.info("Sending OS2faktor challenge to primary client for " + username);
					}
				}

				// challenge ALL clients if no primary was found
				if (challenges.size() == 0) {
					for (Client client : clients) {
						SubscriptionInfo challenge = initAuthentication(client.getDeviceId());
						
						challenges.add(challenge);
					}
				}
				
				log.info("Sending OS2faktor challenge to " + challenges.size() + " client" + ((challenges.size() != 1) ? "s" : "") + " for " + username);
				if (log.isDebugEnabled()) {
					for (Client client : clients) {
						log.debug(" - " + client.getName() + " / " + client.getDeviceId() + (client.isPrime() ? " (prime)" : ""));
					}
				}

				// wait up to 60 seconds for one of them to respond
				int counter = 0;
				
				while (counter < 60) {
					counter++;

					for (SubscriptionInfo challenge : challenges) {
						switch (getAuthenticationStatus(challenge)) {
							case ACCEPTED:
								log.info("User accepted challenge: " + username);
								return true;
							case REJETED:
								log.info("User rejected challenge: " + username);
								return false;
							case NONE:
								log.debug("No response yet: " + username);
								break;
						}
					}

					// wait 1 second, and try again
					Thread.sleep(1000);
				}
				
				log.info("Timeout on OS2faktor: " + username);
			}
			else {
				log.warn("Cannot perform OS2faktor login, because user does not have any OS2faktor devices: " + username);
			}
		}
		catch (Exception ex) {
			log.error("Failed to verify password for: " + username, ex);
		}
		
		return false;
	}

	private List<Client> getClients(ClientSearchParams clientParams) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", apiKey);
		headers.add("connectorVersion", connectorVersion);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {			
			String uriString = baseUrl + "/api/server/nsis/clients";
			uriString = addParameter(uriString, "ssn", encodeSsn(clientParams.getSsn()));
			uriString = addParameter(uriString, "deviceId", clientParams.getDeviceId());
			
			log.debug("Performing network call to '" + uriString + "'");
			
			ResponseEntity<List<Client>> response = restTemplate.exchange(URI.create(uriString), HttpMethod.GET, entity, new ParameterizedTypeReference<List<Client>>(){});

			log.debug("call completed with code " + response.getStatusCodeValue());
			
			return response.getBody();
		}
		catch (Exception ex) {
			log.warn("Failed to get a clients: " + ex);
		}
		
		return null;
	}

	private String addParameter(String uri, String key, String value) throws Exception {
		if (value == null) {
			return uri;
		}
			
		StringBuilder builder = new StringBuilder();
		builder.append(uri);
		
		if (uri.contains("?")) {
			builder.append("&");
		}
		else {
			builder.append("?");
		}
		
		builder.append(key);
		builder.append("=");
		builder.append(value);
		
		return builder.toString();
	}

	private SubscriptionInfo initAuthentication(String deviceId) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("apiKey", apiKey);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			ResponseEntity<SubscriptionInfo> response = restTemplate.exchange(baseUrl + "/api/server/client/" + deviceId + "/authenticate", HttpMethod.PUT, entity, SubscriptionInfo.class);

			return response.getBody();
		}
		catch (Exception ex) {
			log.warn("Failed to get a challenge: " + ex.getMessage());
		}
		
		return null;
	}

	private LoginStatus getAuthenticationStatus(SubscriptionInfo info) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("apiKey", apiKey);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<SubscriptionInfo> response = restTemplate.exchange(baseUrl + "/api/server/notification/" + info.getSubscriptionKey() + "/status", HttpMethod.GET, entity, SubscriptionInfo.class);

		info = response.getBody();
		if (info.isClientAuthenticated()) {
			return LoginStatus.ACCEPTED;
		}
		else if (info.isClientRejected()) {
			return LoginStatus.REJETED;
		}
		
		return LoginStatus.NONE;
	}

	private String encodeSsn(String ssn) throws Exception {
		if (ssn == null) {
			return null;
		}

		// remove slashes
		ssn = ssn.replace("-", "");
		
		// digest
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] ssnDigest = md.digest(ssn.getBytes(Charset.forName("UTF-8")));

		// base64 encode
		return Base64.getEncoder().encodeToString(ssnDigest).replace("+", "%2B");
	}
}
