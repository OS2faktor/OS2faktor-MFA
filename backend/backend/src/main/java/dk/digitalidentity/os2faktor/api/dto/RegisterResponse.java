package dk.digitalidentity.os2faktor.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterResponse {
	private boolean success;
	private boolean invalidPin;
	private String deviceId;
	private String apiKey;
}
