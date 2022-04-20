package dk.digitalidentity.os2faktor.service;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.os2faktor.service.model.ClientSearchParams;
import dk.digitalidentity.os2faktor.service.model.IdentifyClaim;
import dk.digitalidentity.os2faktor.service.model.IdentifyResource;
import dk.digitalidentity.os2faktor.service.model.IdentifyResponse;
import dk.digitalidentity.os2faktor.service.model.IdentifyTokenResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IdentifyService {
	private RestTemplate restTemplate = new RestTemplate();
	private Date accessTokenExpires = new Date();
	private String accessToken;

	@Value("${identify.token.url:}")
	private String tokenUrl;

	@Value("${identify.token.clientId:}")
	private String tokenClientId;

	@Value("${identify.token.clientSecret:}")
	private String tokenClientSecret;

	@Value("${identify.token.username:}")
	private String tokenUsername;

	@Value("${identify.token.password:}")
	private String tokenPassword;

	@Value("${identify.lookup.url:}")
	private String lookupUrl;
	
	public ClientSearchParams getUserDetails(String sAMAccountName) throws Exception {
		log.info("Attempting to lookup " + sAMAccountName + " in Identify");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", getAccessToken(sAMAccountName));
        
        HttpEntity<String> request = new HttpEntity<>(headers);

        String filter = "userName eq \"" + sAMAccountName + "\" and active eq \"true\"";
        String url = lookupUrl + "?filter=" + filter + "&sortOrder=Ascending&attributes=urn:scim:schemas:extension:safewhere:identify:1.0";

        try {
	        ResponseEntity<IdentifyResponse> response = restTemplate.exchange(url, HttpMethod.GET, request, IdentifyResponse.class);

	        String ssn = null;
	        IdentifyResponse identifyResponse = response.getBody();
	        for (IdentifyResource resource : identifyResponse.getResources()) {
	        	for (IdentifyClaim claim : resource.getRecord().getClaims()) {
	        		if ("dk:gov:saml:attribute:CprNumberIdentifier".equals(claim.getType())) {
	        			ssn = claim.getValue();
	        			break;
	        		}
	        	}
	        }
	        
	        if (ssn == null) {
	        	log.warn("No dk:gov:saml:attribute:CprNumberIdentifier claim available on " + sAMAccountName);
	        	return null;
	        }
	        
	        ClientSearchParams params = new ClientSearchParams();
	        params.setSsn(ssn);
	
	        return params;
        }
        catch (HttpClientErrorException ex) {
        	log.error("Failed to lookup ssn for " + sAMAccountName + ": " + ex.getResponseBodyAsString());
        	
        	return null;
        }
	}
	
	private String getAccessToken(String sAMAccountName) {
		// semi-smart caching
		if (!StringUtils.isEmpty(accessToken) && new Date().before(accessTokenExpires)) {
			log.debug("Using cached access token");
			return accessToken;
		}

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("scope", "identify*scim");
        body.add("client_id", tokenClientId);
        body.add("client_secret", tokenClientSecret);
        body.add("username", tokenUsername);
        body.add("password", tokenPassword);

        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        // Create and send request
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        try {
        	ResponseEntity<IdentifyTokenResponse> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, IdentifyTokenResponse.class);
        	IdentifyTokenResponse responseBody = response.getBody();

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.SECOND, (responseBody.getExpiresIn() - (10 * 60)));

            accessTokenExpires = cal.getTime();
            accessToken = responseBody.getAccessToken();
        }
        catch (HttpClientErrorException ex) {
        	log.error("Failed to get token for " + sAMAccountName + ": " + ex.getResponseBodyAsString());
        	
        	return null;
        }

        return accessToken;
	}
}
