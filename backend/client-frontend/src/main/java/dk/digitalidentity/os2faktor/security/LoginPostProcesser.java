package dk.digitalidentity.os2faktor.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.saml.interfaces.SamlLoginPostProcessor;
import dk.digitalidentity.saml.service.model.TokenUser;

@Component
public class LoginPostProcesser implements SamlLoginPostProcessor {

	@Autowired
	private SecurityUtil securityUtil;

	@Override
	public void process(TokenUser tokenUser) {
		securityUtil.updateTokenUser(tokenUser);
	}
}
