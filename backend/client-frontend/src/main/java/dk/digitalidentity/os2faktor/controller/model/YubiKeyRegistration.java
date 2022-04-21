package dk.digitalidentity.os2faktor.controller.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class YubiKeyRegistration {
	private ObjectMapper mapper = new ObjectMapper();
	private String response;
	private long id;
	
	public RegisterPayload decode() throws Exception {
		return mapper.readValue(response.getBytes(), RegisterPayload.class);
	}
}
