package dk.digitalidentity.os2faktor.api.dto;

import dk.digitalidentity.os2faktor.dao.model.enums.RegistrationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HardwareTokenDTO {
	private RegistrationStatus status;
	private boolean found;
}
