package dk.digitalidentity.os2faktor.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;

@Component
public class LoginPostProcesser implements SamlLoginPostProcessor {

	@Autowired
	private SecurityUtil securityUtil;

	@Override
	public void process(TokenUser tokenUser) {
		securityUtil.updateTokenUser(tokenUser);
	}
}
