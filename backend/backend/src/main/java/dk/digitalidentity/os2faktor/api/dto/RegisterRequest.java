package dk.digitalidentity.os2faktor.api.dto;

import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
	private String name;
	private ClientType type;
	private String token;
	private String pincode;
}
