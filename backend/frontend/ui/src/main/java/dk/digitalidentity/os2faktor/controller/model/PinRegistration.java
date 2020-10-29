package dk.digitalidentity.os2faktor.controller.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PinRegistration {
	private String pin;
	private String confirm;
}
