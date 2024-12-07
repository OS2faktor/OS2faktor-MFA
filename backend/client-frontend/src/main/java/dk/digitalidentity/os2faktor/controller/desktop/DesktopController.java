package dk.digitalidentity.os2faktor.controller.desktop;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import dk.digitalidentity.os2faktor.controller.BaseController;
import dk.digitalidentity.os2faktor.controller.ControllerUtil;
import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.security.SecurityUtil;
import dk.digitalidentity.os2faktor.service.AccessControlService;
import dk.digitalidentity.os2faktor.service.UserService;
import dk.digitalidentity.os2faktor.service.model.ClientOrUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class DesktopController extends BaseController {
	
	@Autowired
	private AccessControlService accessControlService;

	@Autowired
	private UserService userService;
	
	@Autowired
	private SecurityUtil securityUtil;

	@GetMapping("/")
	public String adminIndex(HttpServletRequest request) {
		if (securityUtil.getTokenUser() != null) {
			return "redirect:/desktop/selfservice";
		}

		return "desktop/login";
	}

	@RequestMapping("/desktop/selfservice")
	public String getClients(Model model, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request);
		if (userOrLoginPage.loginPage != null) {
			return userOrLoginPage.loginPage;
		}

		// reload the user, to get any changes since last visit
		List<Client> clients = userService.getByEncryptedAndEncodedSsn(userOrLoginPage.user.getSsn()).getClients()
					.stream().filter(c -> !c.isDisabled())
					.collect(Collectors.toList());

		model.addAttribute("clients", clients);

		return "desktop/list";
	}

	@GetMapping("/desktop/selfservice/{deviceId}")
	public String getClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
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
			log.error("User " + user.getPid() + " tried to access client " + client.getDeviceId());

			return "redirect:/desktop/selfservice";
		}

		model.addAttribute("client", client);

		return "desktop/client";
	}
	
	@GetMapping("/desktop/selfservice/{deviceId}/delete")
	public String deleteClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
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
		if (access) {
			client.setDisabled(true);
			clientService.save(client);
		}
		else {
			log.error("User " + user.getPid() + " tried to delete client " + client.getDeviceId());
		}
		
		return "redirect:/desktop/selfservice";
	}

	// TODO: this is a copy of the code in SelfServiceController - merge at some point
	@GetMapping("/desktop/selfservice/{deviceId}/prime")
	public String setPrimeClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
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
			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, "User " + user.getPid() + " tried to set " + deviceId + " as prime", PageTarget.DESKTOP);
		}

		if (client.getUser() != null) {
			for (Client c : client.getUser().getClients()) {
				c.setPrime(Objects.equals(c.getDeviceId(), client.getDeviceId()));
			}

			clientService.saveAll(client.getUser().getClients());
		}
		else {
			client.setPrime(true);

			clientService.save(client);
		}

		return "redirect:/desktop/selfservice";
	}

	// TODO: this is a copy of the code in SelfServiceController - merge at some point
	@GetMapping("/desktop/selfservice/{deviceId}/notprime")
	public String unsetPrimeClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
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
			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, "User " + user.getPid() + " tried to set " + deviceId + " as prime", PageTarget.DESKTOP);
		}

		client.setPrime(false);

		clientService.save(client);

		return "redirect:/desktop/selfservice";
	}
}
