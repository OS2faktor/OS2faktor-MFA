package dk.digitalidentity.os2faktor.service.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClientSearchParams{
	private String ssn;
	private String deviceId;
}