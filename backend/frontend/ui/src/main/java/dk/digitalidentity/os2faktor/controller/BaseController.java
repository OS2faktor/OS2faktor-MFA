package dk.digitalidentity.os2faktor.controller;

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

public abstract class BaseController {

	@Autowired
	protected ClientDao clientDao;

	protected ClientOrErrorPage authenticateClient(Model model, FailedFlow flow, String deviceId, String apiKey) {
		ClientOrErrorPage result = new ClientOrErrorPage();

		result.client = clientDao.getByDeviceId(deviceId);
		if (result.client == null) {
			result.errorPage = ControllerUtil.handleError(model, flow, ErrorType.UNKNOWN_CLIENT, "No client found with deviceId=" + deviceId, PageTarget.APP);
		}
		else if (!result.client.getApiKey().equals(apiKey)) {
			result.client = null;
			result.errorPage = ControllerUtil.handleError(model, flow, ErrorType.BAD_CREDENTIALS, "Incorrect ApiKey for deviceId=" + deviceId, PageTarget.APP);
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
		UserOrLoginPage result = new UserOrLoginPage();
		
		// check for authenticated user
		Object o = request.getSession().getAttribute(ClientSecurityFilter.SESSION_USER);
		if (o == null || !(o instanceof User)) {
			result.loginPage = "redirect:/ui/desktop/selfservice/login";
		}
		else {
			result.user = (User) o;
		}
		
		return result;
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