package dk.digitalidentity.os2faktor.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
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
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.ExternalLoginSessionService;
import dk.digitalidentity.os2faktor.service.UserService;

@Controller
public class ExternalLoginController {

	@Autowired
	private ExternalLoginSessionService externalLoginSessionService;

	@Autowired
	private UserService userService;

	@GetMapping("/external/yubikeyHandoff/{sessionKey}")
	public String yubikeyHandoff(Model model, @PathVariable("sessionKey") String sessionKey, @RequestParam(required = true) String redirect, HttpServletRequest request) throws Exception {
		ExternalLoginSession externalLoginSession  = externalLoginSessionService.findBySessionKey(sessionKey);
		if (externalLoginSession == null) {
			return ControllerUtil.handleError(model, FailedFlow.EXTERNAL_LOGIN, ErrorType.UNAUTHORIZED, "supplied sessionKey did not exist: " + sessionKey, PageTarget.DESKTOP);
		}

		if (externalLoginSession.getTts() == null || new Date().after(externalLoginSession.getTts())) {
			return ControllerUtil.handleError(model, FailedFlow.EXTERNAL_LOGIN, ErrorType.EXTERNAL_SESSION_EXPIRED, "session for supplied key has expired.", PageTarget.DESKTOP);
		}
		
		User user = userService.getByEncryptedAndEncodedSsn(externalLoginSession.getSsn());
		if (user == null) {
        	user = new User();
        	user.setClients(new ArrayList<Client>());
            user.setPid("external-" + externalLoginSession.getLoginServiceProvider().getCvr() + "-" + UUID.randomUUID().toString());
            user.setSsn(externalLoginSession.getSsn());

        	user = userService.save(user);
		}

		// store authenticated user on session
		HttpSession session = request.getSession();
		session.setAttribute(ClientSecurityFilter.SESSION_USER, user);
		session.setAttribute(ClientSecurityFilter.SESSION_REDIRECTURL, redirect);
		session.setAttribute(ClientSecurityFilter.SESSION_CVR, externalLoginSession.getLoginServiceProvider().getCvr());
		session.setAttribute(ClientSecurityFilter.SESSION_NSIS_LEVEL, externalLoginSession.getNsisLevel().toString());

		// note that we explicitly do not give the ROLE_USER grant here, to prevent
		// access to any other pages (which all require that specific grant)
		
		return "redirect:/ui/yubikey";
	}
}
