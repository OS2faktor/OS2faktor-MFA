package dk.digitalidentity.os2faktor.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.AccessControlService;
import dk.digitalidentity.os2faktor.service.NemIDService;
import dk.digitalidentity.os2faktor.service.UserService;
import dk.digitalidentity.os2faktor.service.model.ClientOrUser;
import dk.digitalidentity.os2faktor.service.model.PidAndCprOrError;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class DesktopController extends BaseController {
	
	@Autowired
	private AccessControlService accessControlService;

	@Autowired
	private UserService userService;
	
	@Autowired
	private NemIDService nemIDService;

	@RequestMapping("/ui/desktop/selfservice")
	public String getClients(Model model, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request);
		if (userOrLoginPage.loginPage != null) {
			return userOrLoginPage.loginPage;
		}

		// reload the user, to get any changes since last visit
		List<Client> clients = userService.getByPid(userOrLoginPage.user.getPid()).getClients()
					.stream().filter(c -> !c.isDisabled())
					.collect(Collectors.toList());

		model.addAttribute("clients", clients);

		return "desktop/list";
	}
	
	@GetMapping("/ui/desktop/logoff")
	public String logout(HttpServletRequest request) {
		request.getSession().removeAttribute(ClientSecurityFilter.SESSION_USER);
		
		return "redirect:https://www.os2faktor.dk/";
	}
	
	@GetMapping("/ui/desktop/selfservice/login")
	public String loginGet(Model model, HttpServletRequest request) {
		nemIDService.populateModel(model, request);

		return "desktop/nemid";
	}

	@PostMapping("/ui/desktop/selfservice/login")
	public String loginPost(Model model, @RequestParam Map<String, String> map, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String responseB64 = map.get("response");
		PidAndCprOrError result = nemIDService.verify(responseB64, request);

		if (result.hasError()) {
			if (result.getError().equals(ErrorType.NEMID_VALIDATION_KNOWN_ERROR)) {
				return ControllerUtil.handleKnownNemIDError(model, FailedFlow.NEMID, ErrorType.NEMID_VALIDATION, result.getErrorCode(), PageTarget.DESKTOP);
			}
			
			return ControllerUtil.handleError(model, FailedFlow.NEMID, result.getError(), result.getErrorCode(), PageTarget.DESKTOP);
		}

		String cpr = result.getCpr();
		String pid = result.getPid();

		// if the user does not exist, create the user
        User user = userService.getByPlainTextSsn(cpr);
        if (user == null) {
        	user = new User();
        	user.setClients(new ArrayList<Client>());		            
            user.setPid(pid);
            user.setSsn(cpr);

            userService.save(user);
        }

        // store authenticated user on session
        // TODO: make user serializeable, so we can store in session
		request.getSession().setAttribute(ClientSecurityFilter.SESSION_USER, user);

		return "redirect:/ui/desktop/selfservice";
	}
		
	@GetMapping("/ui/desktop/selfservice/{deviceId}")
	public String getClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request);
		if (userOrLoginPage.loginPage != null) {
			return userOrLoginPage.loginPage;
		}

		Client client = clientDao.getByDeviceId(deviceId);
		if (client == null) {
			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, "supplied deviceId did not exist: " + deviceId, PageTarget.DESKTOP);
		}

		User user = userOrLoginPage.user;
		boolean access = accessControlService.doesAuthenticatedEntityHaveAccessToDeviceId(new ClientOrUser(user), deviceId);
		if (!access) {
			log.error("User " + user.getPid() + " tried to access client " + client.getDeviceId());

			return "redirect:/ui/desktop/selfservice";
		}

		model.addAttribute("client", client);

		return "desktop/client";
	}
	
	@GetMapping("/ui/desktop/selfservice/{deviceId}/delete")
	public String deleteClient(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request);
		if (userOrLoginPage.loginPage != null) {
			return userOrLoginPage.loginPage;
		}

		Client client = clientDao.getByDeviceId(deviceId);
		if (client == null) {
			return ControllerUtil.handleError(model, FailedFlow.SELF_SERVICE, ErrorType.UNKNOWN_CLIENT, "supplied deviceId did not exist: " + deviceId, PageTarget.DESKTOP);
		}

		User user = userOrLoginPage.user;
		boolean access = accessControlService.doesAuthenticatedEntityHaveAccessToDeviceId(new ClientOrUser(user), deviceId);
		if (access) {
			client.setDisabled(true);
			clientDao.save(client);
		}
		else {
			log.error("User " + user.getPid() + " tried to delete client " + client.getDeviceId());
		}
		
		return "redirect:/ui/desktop/selfservice";
	}
}
