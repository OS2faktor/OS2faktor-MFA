package dk.digitalidentity.os2faktor.api.dto;

import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NSISClientDTO {
	private String deviceId;
	private ClientType type;
	private String name;
	private boolean hasPincode;
	private NSISLevel nsisLevel;
	private boolean prime;
	private boolean roaming;
}
