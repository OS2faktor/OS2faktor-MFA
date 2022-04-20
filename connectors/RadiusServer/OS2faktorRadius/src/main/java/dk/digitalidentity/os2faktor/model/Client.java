package dk.digitalidentity.os2faktor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Client {
	private String name;
	private String deviceId;
	private String type;
	private boolean hasPincode;
	private boolean prime;
}
