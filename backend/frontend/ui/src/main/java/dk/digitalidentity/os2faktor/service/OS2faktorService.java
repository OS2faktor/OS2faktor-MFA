package dk.digitalidentity.os2faktor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.os2faktor.dao.model.Notification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OS2faktorService {
	private RestTemplate restTemplate = new RestTemplate();

	@Value("${backend.baseurl}")
	private String baseUrl;
	
	@Value("${backend.apikey}")
	private String apiKey;
	
	public Notification getChallenge(String deviceId) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("apiKey", apiKey);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			ResponseEntity<Notification> response = restTemplate.exchange(baseUrl + "/api/server/client/" + deviceId + "/authenticate", HttpMethod.PUT, entity, Notification.class);

			return response.getBody();
		}
		catch (Exception ex) {
			log.warn("Failed to get a challenge: " + ex.getMessage());
		}
		
		return null;
	}

	public boolean loginCompleted(Notification info) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("apiKey", apiKey);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			ResponseEntity<Notification> response = restTemplate.exchange(baseUrl + "/api/server/notification/" + info.getSubscriptionKey() + "/status", HttpMethod.GET, entity, Notification.class);
	
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
}
