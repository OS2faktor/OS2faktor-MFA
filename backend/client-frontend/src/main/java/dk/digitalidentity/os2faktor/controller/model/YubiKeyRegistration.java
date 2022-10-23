package dk.digitalidentity.os2faktor.controller.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class YubiKeyRegistration {
	private long id;
	private String response;
}
