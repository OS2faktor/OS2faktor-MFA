package dk.digitalidentity.os2faktor.service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientSearchParams{
	private String ssn;
	private String pid;
	private String pseudonym;
	private String deviceId;
}