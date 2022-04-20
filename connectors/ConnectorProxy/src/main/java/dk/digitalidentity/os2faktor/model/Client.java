package dk.digitalidentity.os2faktor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dk.digitalidentity.os2faktor.model.enums.ClientType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Client {
	private String name;
	private String deviceId;
	private ClientType type;
	private boolean hasPincode;
}
