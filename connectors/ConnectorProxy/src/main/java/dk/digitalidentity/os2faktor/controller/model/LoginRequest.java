package dk.digitalidentity.os2faktor.controller.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
	private String uid;
	private String nonce;
}
