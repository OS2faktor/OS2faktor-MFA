package dk.digitalidentity.os2faktor.api.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Challenge {
	private String uuid;
	private String challenge;
	private String serverName;
}
