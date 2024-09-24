package dk.digitalidentity.os2faktor.service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OS2faktorResponse {
	private String cpr;
	private String name;
	private String email;
	private String samAccountName;
}
