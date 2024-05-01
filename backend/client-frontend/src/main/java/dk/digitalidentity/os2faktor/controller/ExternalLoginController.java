package dk.digitalidentity.os2faktor.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.ExternalLoginSession;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.ExternalLoginSessionService;
import dk.digitalidentity.os2faktor.service.UserService;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;

@Controller
public class ExternalLoginController {

	@Autowired
	private ExternalLoginSessionService externalLoginSessionService;

	@Autowired
	private UserService userService;

	@GetMapping("/external/yubikeyHandoff/{sessionKey}")
	public String yubikeyHandoff(Model model, @PathVariable("sessionKey") String sessionKey, @RequestParam(required = true) String redirect, HttpServletRequest request) throws Exception {
		ExternalLoginSession externalLoginSession = externalLoginSessionService.findBySessionKey(sessionKey);
		if (externalLoginSession == null) {
			return ControllerUtil.handleError(model, FailedFlow.EXTERNAL_LOGIN, ErrorType.UNAUTHORIZED, "supplied sessionKey did not exist: " + sessionKey, PageTarget.DESKTOP);
		}

		if (externalLoginSession.getTts() == null || new Date().after(externalLoginSession.getTts())) {
			return ControllerUtil.handleError(model, FailedFlow.EXTERNAL_LOGIN, ErrorType.EXTERNAL_SESSION_EXPIRED, "session for supplied key has expired.", PageTarget.DESKTOP);
		}
		
		User user = getOrGenerateUser(externalLoginSession);		
		loginUser(request, externalLoginSession, user, redirect);
		
		return "redirect:/yubikey";
	}
	
	@GetMapping("/external/authenticatorHandoff/{sessionKey}")
	public String authenticatorHandoff(Model model, @PathVariable("sessionKey") String sessionKey, @RequestParam(required = true) String redirect, HttpServletRequest request) throws Exception {
		ExternalLoginSession externalLoginSession  = externalLoginSessionService.findBySessionKey(sessionKey);
		if (externalLoginSession == null) {
			return ControllerUtil.handleError(model, FailedFlow.EXTERNAL_LOGIN, ErrorType.UNAUTHORIZED, "supplied sessionKey did not exist: " + sessionKey, PageTarget.DESKTOP);
		}

		if (externalLoginSession.getTts() == null || new Date().after(externalLoginSession.getTts())) {
			return ControllerUtil.handleError(model, FailedFlow.EXTERNAL_LOGIN, ErrorType.EXTERNAL_SESSION_EXPIRED, "session for supplied key has expired.", PageTarget.DESKTOP);
		}
		
		User user = getOrGenerateUser(externalLoginSession);		
		loginUser(request, externalLoginSession, user, redirect);
		
		return "redirect:/authenticator";
	}

	@GetMapping("/external/kodeviserHandoff/{sessionKey}")
	public String hardwareTokenHandoff(Model model, @PathVariable("sessionKey") String sessionKey, @RequestParam(required = true) String redirect, HttpServletRequest request) throws Exception {
		ExternalLoginSession externalLoginSession  = externalLoginSessionService.findBySessionKey(sessionKey);
		if (externalLoginSession == null) {
			return ControllerUtil.handleError(model, FailedFlow.EXTERNAL_LOGIN, ErrorType.UNAUTHORIZED, "supplied sessionKey did not exist: " + sessionKey, PageTarget.DESKTOP);
		}

		if (externalLoginSession.getTts() == null || new Date().after(externalLoginSession.getTts())) {
			return ControllerUtil.handleError(model, FailedFlow.EXTERNAL_LOGIN, ErrorType.EXTERNAL_SESSION_EXPIRED, "session for supplied key has expired.", PageTarget.DESKTOP);
		}
		
		User user = getOrGenerateUser(externalLoginSession);		
		loginUser(request, externalLoginSession, user, redirect);
		
		return "redirect:/totph";
	}
	
	private void loginUser(HttpServletRequest request, ExternalLoginSession externalLoginSession, User user, String redirect) {
		ArrayList<SamlGrantedAuthority> authorities = new ArrayList<>();

		// make SAML filter happy if NSIS level is substantial (anything less, and we let the SAML filter send the user to NemLog-in for step-up)
		if (externalLoginSession.getNsisLevel().equals(NSISLevel.SUBSTANTIAL) || externalLoginSession.getNsisLevel().equals(NSISLevel.HIGH)) {
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("https://data.gov.dk/concept/core/nsis/loa", externalLoginSession.getNsisLevel().toClaimValue());
	
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getSsn(), null, authorities);
			token.setDetails(TokenUser.builder()
					.cvr(externalLoginSession.getLoginServiceProvider().getCvr())
					.username(user.getSsn())
					.attributes(attributes)
					.authorities(authorities)
					.build());

			SecurityContextHolder.getContext().setAuthentication(token);
		}

		HttpSession session = request.getSession();
        session.setAttribute(ClientSecurityFilter.SESSION_USER, user);
        // note that we explicitly do not give the ROLE_USER grant here, to prevent access to any other pages
        // session.setAttribute(ClientSecurityFilter.SESSION_ROLE, "ROLE_USER");
        session.setAttribute(ClientSecurityFilter.SESSION_ROLE, "ROLE_EXTERNAL");
		session.setAttribute(ClientSecurityFilter.SESSION_REDIRECTURL, redirect);
		session.setAttribute(ClientSecurityFilter.SESSION_REDIRECTURL_TTS, LocalDateTime.now());
		session.setAttribute(ClientSecurityFilter.SESSION_CVR, externalLoginSession.getLoginServiceProvider().getCvr());
		session.setAttribute(ClientSecurityFilter.SESSION_NSIS_LEVEL, externalLoginSession.getNsisLevel().toString());
	}
	
	private User getOrGenerateUser(ExternalLoginSession externalLoginSession) throws Exception {
		User user = userService.getByEncryptedAndEncodedSsn(externalLoginSession.getSsn());

		if (user == null) {
			user = new User();
			user.setClients(new ArrayList<Client>());
			user.setPid("external-" + externalLoginSession.getLoginServiceProvider().getCvr() + "-" + UUID.randomUUID().toString());
			user.setSsn(externalLoginSession.getSsn());

			user = userService.save(user);
		}

		return user;
	}
}
