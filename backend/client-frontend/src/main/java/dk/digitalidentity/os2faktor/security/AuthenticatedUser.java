package dk.digitalidentity.os2faktor.security;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticatedUser {
	public static final String ROLE_ADMIN = "ROLE_ADMIN";

	private String name;
	private String uuid;
	private String cvr;
}
