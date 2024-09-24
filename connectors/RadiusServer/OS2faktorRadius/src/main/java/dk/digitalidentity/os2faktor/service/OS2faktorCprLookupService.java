package dk.digitalidentity.os2faktor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.os2faktor.service.model.ClientSearchParams;
import dk.digitalidentity.os2faktor.service.model.OS2faktorResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OS2faktorCprLookupService {
	private RestTemplate restTemplate = new RestTemplate();

	@Value("${os2faktor.cprlookup.url:}")
	private String serviceUrl;

	@Value("${os2faktor.cprlookup.domain:}")
	private String serviceDomain;

	@Value("${os2faktor.cprlookup.apiKey:}")
	private String serviceApiKey;

	public ClientSearchParams getUserDetails(String sAMAccountName) throws Exception {
		log.info("Attempting to lookup " + sAMAccountName + " in OS2faktor");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("ApiKey", serviceApiKey);
        
        HttpEntity<String> request = new HttpEntity<>(headers);

        String url = serviceUrl + "?userId=" + sAMAccountName + "&domain=" + serviceDomain;

        try {
	        ResponseEntity<OS2faktorResponse> response = restTemplate.exchange(url, HttpMethod.GET, request, OS2faktorResponse.class);
	        if (response.getStatusCodeValue() > 299) {
	        	throw new HttpClientErrorException(response.getStatusCode());
	        }

	        OS2faktorResponse os2faktorResponse = response.getBody();
	        String ssn = os2faktorResponse.getCpr();

	        ClientSearchParams params = new ClientSearchParams();
	        params.setSsn(ssn);
	
	        return params;
        }
        catch (HttpClientErrorException ex) {
        	log.error("Failed to lookup ssn for " + sAMAccountName + ": " + ex.getResponseBodyAsString());
        	
        	return null;
        }
	}
}
