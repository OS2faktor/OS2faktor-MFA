package dk.digitalidentity.os2faktor.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import dk.digitalidentity.saml.model.TokenUser;

@Component
public class SecurityUtil {

	public static AuthenticatedUser getUser() {
		AuthenticatedUser user = null;

		if (isUserLoggedIn()) {
			user = (AuthenticatedUser) ((TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails()).getAttributes().get("user");
		}

		return user;
	}

	private static boolean isUserLoggedIn() {
		if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().getDetails() != null && SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser) {
			return true;
		}

		return false;
	}
}
