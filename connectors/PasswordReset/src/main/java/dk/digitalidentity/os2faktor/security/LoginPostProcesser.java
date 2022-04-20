package dk.digitalidentity.os2faktor.security;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dk.digitalidentity.os2faktor.config.Constants;
import dk.digitalidentity.saml.extension.SamlLoginPostProcessor;
import dk.digitalidentity.saml.model.TokenUser;

@Component
public class LoginPostProcesser implements SamlLoginPostProcessor {

	@Override
	public void process(TokenUser tokenUser) {
		HttpServletRequest request = getRequest();
		if (request != null) {
			request.getSession().setAttribute(Constants.SESSION_SAMACCOUNTNAMES, Collections.singletonList(tokenUser.getUsername()));
			request.getSession().setAttribute(Constants.SESSION_SAMACCOUNTNAME, tokenUser.getUsername());
		}
	}
	
	private static HttpServletRequest getRequest() {
		try {
			return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}
}
