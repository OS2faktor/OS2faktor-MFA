package dk.digitalidentity.os2faktor.api.dto;

import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticateUserRequestBody {
	private String cpr;
	private NSISLevel nsisLevel;
}
