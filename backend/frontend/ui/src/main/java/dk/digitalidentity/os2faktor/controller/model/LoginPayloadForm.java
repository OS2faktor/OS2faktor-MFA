package dk.digitalidentity.os2faktor.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginPayloadForm {

	@JsonIgnore
	private ObjectMapper mapper = new ObjectMapper();

	private String response;
	
	public LoginPayload decode() throws Exception {
		return mapper.readValue(response.getBytes(), LoginPayload.class);
	}
}
