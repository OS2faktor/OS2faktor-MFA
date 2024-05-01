package dk.digitalidentity.os2faktor.controller.mobile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.digitalidentity.os2faktor.controller.BaseController;
import dk.digitalidentity.os2faktor.controller.ControllerUtil;
import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.service.AccessControlService;
import dk.digitalidentity.os2faktor.service.model.ClientOrUser;

@Controller
public class SelfServiceController extends BaseController {
	
	@Autowired
	private AccessControlService accessControlService;
	
	@GetMapping("/ui/selfservice")
	public String getClients(Model model, HttpServletRequest request) {
		ClientOrErrorPage clientOrErrorPage = authenticateClient(model, FailedFlow.SELF_SERVICE, request);
		if (clientOrErrorPage.errorPage != null) {
			return clientOrErrorPage.errorPage;
		}

		Client client = clientOrErrorPage.client;

		// reload the client to get any changes since last visit
		List<Client> clients = null;
		Client reloadedClient = clientService.getByDeviceId(client.getDeviceId());

		if (reloadedClient.getUser() != null) {
			clients = reloadedClient.getUser().getClients()
					.stream().filter(c -> c.isDisabled() == false)
					.collect(Collectors.toList());
		}
		else {
			clients = new ArrayList<>();

			if (!client.isDisabled()) {
				clients.add(client);
			}
		}
		
		model.addAttribute("clients", clients);

		return "list";
	}

	@GetMapping("/ui/selfservice/{deviceId}")
	public String getClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
		ClientOrErrorPage clientOrErrorPage = authenticateClient(model, FailedFlow.SELF_SERVICE, request);
		if (clientOrErrorPage.errorPage != null) {
			return clientOrErrorPage.errorPage;
		}

		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, "supplied deviceId did not exist: " + deviceId, PageTarget.APP);
		}

		boolean access = accessControlService.doesAuthenticatedEntityHaveAccessToDeviceId(new ClientOrUser(client), deviceId);
		if (!access) {
			StringBuilder msg = new StringBuilder();
			msg.append("supplied deviceId is not paired with authenticated client! ");
			msg.append("supplied deviceId=" + deviceId + ", ");
			msg.append("authenticated deviceId=" + client.getDeviceId());

			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, msg.toString(), PageTarget.APP);
		}

		model.addAttribute("client", client);

		return "client";
	}
	
	@GetMapping("/ui/selfservice/{deviceId}/delete")
	public String deleteClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
		ClientOrErrorPage clientOrErrorPage = authenticateClient(model, FailedFlow.SELF_SERVICE, request);
		if (clientOrErrorPage.errorPage != null) {
			return clientOrErrorPage.errorPage;
		}

		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, "supplied deviceId did not exist: " + deviceId, PageTarget.APP);
		}

		boolean access = accessControlService.doesAuthenticatedEntityHaveAccessToDeviceId(new ClientOrUser(client), deviceId);
		if (!access) {
			StringBuilder msg = new StringBuilder();
			msg.append("supplied deviceId is not paired with authenticated client! ");
			msg.append("supplied deviceId=" + deviceId + ", ");
			msg.append("authenticated deviceId=" + client.getDeviceId());

			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, msg.toString(), PageTarget.APP);
		}

		client.setDisabled(true);
		clientService.save(client);

		return "redirect:/ui/selfservice";
	}

	// TODO: this is a copy of the code in DesktopController, merge at some point
	@GetMapping("/ui/selfservice/{deviceId}/prime")
	public String setPrimeClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
		ClientOrErrorPage clientOrErrorPage = authenticateClient(model, FailedFlow.SELF_SERVICE, request);
		if (clientOrErrorPage.errorPage != null) {
			return clientOrErrorPage.errorPage;
		}

		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, "supplied deviceId did not exist: " + deviceId, PageTarget.APP);
		}

		boolean access = accessControlService.doesAuthenticatedEntityHaveAccessToDeviceId(new ClientOrUser(client), deviceId);
		if (!access) {
			StringBuilder msg = new StringBuilder();
			msg.append("supplied deviceId is not paired with authenticated client! ");
			msg.append("supplied deviceId=" + deviceId + ", ");
			msg.append("authenticated deviceId=" + client.getDeviceId());

			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, msg.toString(), PageTarget.APP);
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

		return "redirect:/ui/selfservice";
	}

	// TODO: this is a copy of the code in DesktopController, merge at some point
	@GetMapping("/ui/selfservice/{deviceId}/notprime")
	public String unsetPrimeClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
		ClientOrErrorPage clientOrErrorPage = authenticateClient(model, FailedFlow.SELF_SERVICE, request);
		if (clientOrErrorPage.errorPage != null) {
			return clientOrErrorPage.errorPage;
		}

		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, "supplied deviceId did not exist: " + deviceId, PageTarget.APP);
		}

		boolean access = accessControlService.doesAuthenticatedEntityHaveAccessToDeviceId(new ClientOrUser(client), deviceId);
		if (!access) {
			StringBuilder msg = new StringBuilder();
			msg.append("supplied deviceId is not paired with authenticated client! ");
			msg.append("supplied deviceId=" + deviceId + ", ");
			msg.append("authenticated deviceId=" + client.getDeviceId());

			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, msg.toString(), PageTarget.APP);
		}

		client.setPrime(false);
		clientService.save(client);

		return "redirect:/ui/selfservice";
	}
}
