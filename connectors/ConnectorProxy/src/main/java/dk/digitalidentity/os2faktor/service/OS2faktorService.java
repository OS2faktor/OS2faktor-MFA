package dk.digitalidentity.os2faktor.service;

import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.os2faktor.model.Client;
import dk.digitalidentity.os2faktor.service.model.ClientSearchParams;
import dk.digitalidentity.os2faktor.service.model.SubscriptionInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OS2faktorService {
	private RestTemplate restTemplate = new RestTemplate();

	@Value("${login.backend.baseurl}")
	private String baseUrl;

	@Value("${login.backend.apikey}")
	private String apiKey;
	
	@Value("${login.connector.version}")
	private String connectorVersion;

	public List<Client> getClients(ClientSearchParams clientParams) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", apiKey);
		headers.add("connectorVersion", connectorVersion);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			
			String uriString = baseUrl + "/api/server/clients";
			uriString = addParameter(uriString, "ssn", encodeSsn(clientParams.getSsn()));
			uriString = addParameter(uriString, "pid", clientParams.getPid());
			uriString = addParameter(uriString, "pseudonym", clientParams.getPseudonym());
			uriString = addParameter(uriString, "deviceId", clientParams.getDeviceId());
			
			ResponseEntity<List<Client>> response = restTemplate.exchange(URI.create(uriString), HttpMethod.GET, entity, new ParameterizedTypeReference<List<Client>>(){});
			
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

	public SubscriptionInfo getChallenge(String deviceId) {
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

	public boolean loginCompleted(SubscriptionInfo info) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("apiKey", apiKey);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			ResponseEntity<SubscriptionInfo> response = restTemplate.exchange(baseUrl + "/api/server/notification/" + info.getSubscriptionKey() + "/status", HttpMethod.GET, entity, SubscriptionInfo.class);
	
			info = response.getBody();
			if (info.isClientAuthenticated()) {
				return true;
			}
		}
		catch (Exception ex) {
			log.warn("Failed to verify login: " + ex.getMessage());
		}
		
		return false;
	}
	

	private String encodeSsn(String ssn) throws Exception {
		if(ssn ==null) {
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
