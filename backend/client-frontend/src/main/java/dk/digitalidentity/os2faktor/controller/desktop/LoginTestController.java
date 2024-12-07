package dk.digitalidentity.os2faktor.controller.desktop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.os2faktor.controller.BaseController;
import dk.digitalidentity.os2faktor.controller.ControllerUtil;
import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.AccessControlService;
import dk.digitalidentity.os2faktor.service.OS2faktorService;
import dk.digitalidentity.os2faktor.service.model.ClientOrUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class LoginTestController extends BaseController {
	
	@Autowired
	private AccessControlService accessControlService;
	
	@Autowired
	private OS2faktorService os2FaktorService;

	@Value("${backend.baseurl}")
	private String baseUrl;

	@GetMapping("/desktop/test/{deviceId}")
	public String initTestLogin(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request);
		if (userOrLoginPage.loginPage != null) {
			return userOrLoginPage.loginPage;
		}

		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, "supplied deviceId did not exist: " + deviceId, PageTarget.DESKTOP);
		}

		User user = userOrLoginPage.user;
		boolean access = accessControlService.doesAuthenticatedEntityHaveAccessToDeviceId(new ClientOrUser(user), deviceId);
		if (!access) {
			log.error("User " + user.getPid() + " tried to test client " + client.getDeviceId());

			return "redirect:/desktop/selfservice";
		}

		model.addAttribute("client", client);

		// get a challenge for this client from the backend
		Notification info = os2FaktorService.getChallenge(client.getDeviceId());
		if (info == null) {
			log.error("Cannot test login, backend appears to be down: clientID=" + client.getDeviceId());
			return "redirect:/desktop/selfservice";
		}

		// TODO: Notification class should be serializeable, so we can store it in the session
		// store the whole challenge object on the session for later verification
		request.getSession().setAttribute(ClientSecurityFilter.SESSION_TEST_LOGIN, info);

		if (client.getType().equals(ClientType.YUBIKEY) || client.getType().equals(ClientType.TOTP) || client.getType().equals(ClientType.TOTPH)) {
			model.addAttribute("pollingKey", info.getPollingKey());
			model.addAttribute("redirectUrl", info.getRedirectUrl());
			model.addAttribute("baseUrl", baseUrl);
			model.addAttribute("yubikey", client.getType().equals(ClientType.YUBIKEY));
			model.addAttribute("totp", client.getType().equals(ClientType.TOTP));
			model.addAttribute("totph", client.getType().equals(ClientType.TOTPH));
		}
		else {	
			model.addAttribute("pollingKey", info.getPollingKey());
			model.addAttribute("challenge", info.getChallenge());
			model.addAttribute("baseUrl", baseUrl);
		}

		return "desktop/testauth";
	}
	
	@PostMapping("/desktop/test")
	public String verifyTestLogin(Model model, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request);
		if (userOrLoginPage.loginPage != null) {
			return userOrLoginPage.loginPage;
		}

		// 0: error, 1: approved, 2: rejected
		int res = 0;

		Notification info = (Notification) request.getSession().getAttribute(ClientSecurityFilter.SESSION_TEST_LOGIN);
		if (info != null) {
			boolean success = os2FaktorService.loginCompleted(info);

			if (success) {
				res = 1;
			}
			else {
				res = 2;
			}
		}

		model.addAttribute("result", res);

		return "desktop/testauthresult";
	}
}
