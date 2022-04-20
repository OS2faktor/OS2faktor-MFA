package dk.digitalidentity.os2faktor.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetPinRequest {
	private String oldPin; // can be null
	private String newPin;
}
