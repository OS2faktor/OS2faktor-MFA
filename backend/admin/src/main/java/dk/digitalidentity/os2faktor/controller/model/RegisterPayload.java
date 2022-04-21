package dk.digitalidentity.os2faktor.controller.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterPayload {
	private String id;
	private String attestationObject;
	private String clientDataJson;
}
