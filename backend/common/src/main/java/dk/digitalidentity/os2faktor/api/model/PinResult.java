package dk.digitalidentity.os2faktor.api.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PinResult {
	private PinResultStatus status;
	private String lockedUntil;
}
