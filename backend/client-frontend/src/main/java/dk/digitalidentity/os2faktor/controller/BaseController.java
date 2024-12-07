package dk.digitalidentity.os2faktor.controller;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.ClientService;
import dk.digitalidentity.os2faktor.service.HashingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseController {

	@Autowired
	protected ClientService clientService;

	@Autowired
	protected HashingService hashingService;

	protected NSISLevel getNsisLevel(HttpServletRequest request) {
		NSISLevel nsisLevel = NSISLevel.NONE;
		
		Object oNsisLevel = request.getSession().getAttribute(ClientSecurityFilter.SESSION_NSIS_LEVEL);
		if (oNsisLevel != null && oNsisLevel instanceof String && ((String)oNsisLevel).length() > 0) {
			try {
				nsisLevel = NSISLevel.valueOf((String) oNsisLevel);
			}
			catch (Exception ex) {
				log.error("Unable to parse loa: " + oNsisLevel.toString());
			}
		}
		
		return nsisLevel;
	}

	protected ClientOrErrorPage authenticateClient(Model model, FailedFlow flow, String deviceId, String apiKey) {
		ClientOrErrorPage result = new ClientOrErrorPage();

		result.client = clientService.getByDeviceId(deviceId);
		if (result.client == null) {
			result.errorPage = ControllerUtil.handleError(model, flow, ErrorType.UNKNOWN_CLIENT, "No client found with deviceId=" + deviceId, PageTarget.APP);
		}
		else {
			try {
				if (!hashingService.matches(apiKey, result.client.getApiKey())) {
					result.client = null;
					result.errorPage = ControllerUtil.handleError(model, flow, ErrorType.BAD_CREDENTIALS, "Incorrect ApiKey for deviceId=" + deviceId, PageTarget.APP);
				}
			}
			catch (Exception exception) {
				result.client = null;
				result.errorPage = ControllerUtil.handleError(model, flow, ErrorType.EXCEPTION, "Failed to match apiKey to encrypted and encoded apiKey", PageTarget.APP);
			}
		}
		
		return result;
	}

	protected ClientOrErrorPage authenticateClient(Model model, FailedFlow flow, HttpServletRequest request) {
		ClientOrErrorPage result = new ClientOrErrorPage();
		
		// check for authenticated client
		Object o = request.getSession().getAttribute(ClientSecurityFilter.SESSION_CLIENT);
		if (o == null || !(o instanceof Client)) {
			Object e = request.getSession().getAttribute(ClientSecurityFilter.SESSION_ERROR);

			if (e != null && e instanceof ErrorType) {
				result.errorPage = ControllerUtil.handleError(model, flow, (ErrorType) e, null, PageTarget.APP);
			}
			else {
				result.errorPage = ControllerUtil.handleError(model, flow, ErrorType.UNKNOWN, null, PageTarget.APP);
			}
		}
		else {
			result.client = (Client) o;
		}
		
		return result;
	}

	protected UserOrLoginPage authenticateUser(HttpServletRequest request) {
		return authenticateUser(request, true);
	}
	
	protected UserOrLoginPage authenticateUser(HttpServletRequest request, boolean requireUserRole) {
		UserOrLoginPage result = new UserOrLoginPage();

		// check for authenticated user
		HttpSession session = request.getSession();
		Object o = session.getAttribute(ClientSecurityFilter.SESSION_USER);
		if (o == null || !(o instanceof User)) {
			log.warn("No authenticated user on session - rejecting access to page");

			result.loginPage = "redirect:/";
		}
		else if (requireUserRole && !hasRole(request, "ROLE_USER")) {
			log.warn("Authenticated user does not have ROLE_USER - rejecting access");

			result.loginPage = "redirect:/";
		}
		else {
			result.user = (User) o;
		}

		return result;
	}
	
	private boolean hasRole(HttpServletRequest request, String role) {
        Object o = request.getSession().getAttribute(ClientSecurityFilter.SESSION_ROLE);
        if (o == null || !(o instanceof String) || !Objects.equals(role, (String)o)) {
        	return false;
        }
        
        return true;
	}

	public class ClientOrErrorPage {
		public Client client;
		public String errorPage;
	}
	
	public class UserOrLoginPage {
		public User user;
		public String loginPage;
	}
}