package dk.digitalidentity.os2faktor.controller;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.HashingService;
import lombok.extern.log4j.Log4j;

@Log4j
public abstract class BaseController {

	@Autowired
	protected ClientDao clientDao;

	@Autowired
	protected HashingService hashingService;

	protected ClientOrErrorPage authenticateClient(Model model, FailedFlow flow, String deviceId, String apiKey) {
		ClientOrErrorPage result = new ClientOrErrorPage();

		result.client = clientDao.getByDeviceId(deviceId);
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
		Object o = request.getSession().getAttribute(ClientSecurityFilter.SESSION_USER);
		if (o == null || !(o instanceof User)) {
			log.warn("No authenticated user on session - rejecting access to page");
			result.loginPage = "redirect:/ui/desktop/selfservice/login";
		}
		else if (requireUserRole && !hasRole(request, "ROLE_USER")) {
			log.warn("Authenticated user does not have ROLE_USER - rejecting access");
			result.loginPage = "redirect:/ui/desktop/selfservice/login";
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

	class ClientOrErrorPage {
		Client client;
		String errorPage;
	}
	
	class UserOrLoginPage {
		User user;
		String loginPage;
	}
}