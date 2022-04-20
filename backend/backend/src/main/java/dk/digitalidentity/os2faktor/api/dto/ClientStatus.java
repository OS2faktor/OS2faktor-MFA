package dk.digitalidentity.os2faktor.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientStatus {
	private boolean disabled;
	private boolean pinProtected;
	private boolean nemIdRegistered;
}
