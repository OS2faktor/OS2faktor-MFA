package dk.digitalidentity.os2faktor.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.openoces.ooapi.exceptions.NonOcesCertificateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.controller.model.Registration;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.IdGenerator;
import dk.digitalidentity.os2faktor.service.LocalClientService;
import dk.digitalidentity.os2faktor.service.NemIDService;
import dk.digitalidentity.os2faktor.service.PushNotificationSenderService;
import dk.digitalidentity.os2faktor.service.UserService;
import dk.digitalidentity.os2faktor.service.model.PidAndCprOrError;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class Registration2Controller extends BaseController {
	
	@Autowired
	private PushNotificationSenderService snsService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private NemIDService nemIDService;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private LocalClientService localClientService;

	// updated registration flow with NemID inlined
	// basically a copy of the registrationController, which can be deleted once all clients are updated (beware ancient windows clients)
	
	@GetMapping("/ui/register2")
	public String registerGet(Model model, @RequestParam(name = "token", required = false) String token, @RequestParam(name = "type") ClientType type, HttpServletRequest request) {
		Registration registration = new Registration();

		model.addAttribute("registration", registration);

		request.getSession().setAttribute(ClientSecurityFilter.SESSION_TYPE, type);
		if (token != null) {
			request.getSession().setAttribute(ClientSecurityFilter.SESSION_TOKEN, token);
		}

		return "register2";
	}
	
	@PostMapping("/ui/register2")
	public String registerPost(Model model, @ModelAttribute("registration") @Valid Registration registration, BindingResult bindingResult, HttpServletRequest request) {
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("registration", registration);

			return "register2";
		}

		Object token = request.getSession().getAttribute(ClientSecurityFilter.SESSION_TOKEN);
		Object type = request.getSession().getAttribute(ClientSecurityFilter.SESSION_TYPE);

		if (type == null || !(type instanceof ClientType)) {
			return ControllerUtil.handleError(model, FailedFlow.REGISTRATION, ErrorType.BAD_REQUEST, "No type on session!", PageTarget.APP);
		}

		String sToken = (token != null && token instanceof String && ((String) token).length() > 0) ? (String) token : null;
		
		Client client = new Client();
		client.setUseCount(0);
		client.setDeviceId(idGenerator.generateDeviceId());
		client.setApiKey(idGenerator.generateUuid());
		client.setName(registration.getName());
		client.setToken(sToken);
		client.setType((ClientType) type);

		if (sToken != null) {
			try {
				String notificationKey = snsService.createEndpoint(sToken, client.getDeviceId(), (ClientType) type);
				if (!StringUtils.isEmpty(notificationKey)) {
					client.setNotificationKey(notificationKey);
					
					// see if there are any existing clients with this notification key, and disable them
					// as they are old clients installed on the same device (and now overwritten by the new client)
					List<Client> existingClients = clientDao.getByNotificationKey(notificationKey);
					for (Client existingClient : existingClients) {
						// TODO: should probably add the DisabledFalse flag to the query instead for better performance
						if (!existingClient.isDisabled()) {
							existingClient.setDisabled(true);
							clientDao.save(existingClient);
						}
					}
				}
			}
			catch (Exception ex) {
				log.error("Failed to register token at AWS SNS", ex);
				
				return ControllerUtil.handleError(model, FailedFlow.REGISTRATION, ErrorType.EXCEPTION, "Failed to register token at AWS SNS", PageTarget.APP);
			}
		}
		
		clientDao.save(client);
		return "redirect:/ui/register2/nemid?apiKey=" + client.getApiKey() + "&deviceId=" + client.getDeviceId()+"&fromRedirect=true";
	}
	
	@GetMapping("/ui/register2/nemid")
	public String registerNemIdGet(Model model, @RequestParam(name = "deviceId") String deviceId, @RequestParam(name = "apiKey") String apiKey, @RequestParam(required = false, name = "fromRedirect") boolean fromRedirect, HttpServletRequest request) {
		ClientOrErrorPage clientOrErrorPage = authenticateClient(model, FailedFlow.NEMID, deviceId, apiKey);
		if (clientOrErrorPage.errorPage != null) {
			return clientOrErrorPage.errorPage;
		}

		// the called method always returns a client - it cannot return a user
		Client client = clientOrErrorPage.client;		
        User user = client.getUser();
        if (user != null) {
        	return ControllerUtil.handleError(model, FailedFlow.NEMID, ErrorType.ALREADY_REGISTERED, "Client " + deviceId + " already registered to user " + user.getId(), PageTarget.APP);
        }

		request.getSession().setAttribute(ClientSecurityFilter.SESSION_DEVICE_ID, deviceId);
		
		model.addAttribute("apiKey", apiKey);
		model.addAttribute("deviceId", deviceId);
		model.addAttribute("fromRedirect", fromRedirect);
		
		nemIDService.populateModel(model, request);
		return "nemid2";
	}
	
	@PostMapping(value = "/ui/register2/nemid")
	public String login(Model model, @RequestParam Map<String, String> map, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String responseB64 = map.get("response");
		
		PidAndCprOrError result = null;
		try {
			result = nemIDService.verify(responseB64, request);
	
			if (result.hasError()) {
				if (result.getError().equals(ErrorType.NEMID_VALIDATION_KNOWN_ERROR)) {
					return ControllerUtil.handleKnownNemIDError(model, FailedFlow.NEMID, ErrorType.NEMID_VALIDATION, result.getErrorCode(), PageTarget.APP);
				}
				
				return ControllerUtil.handleError(model, FailedFlow.NEMID, result.getError(), result.getErrorCode(), PageTarget.APP);
			}
		}
		catch (NonOcesCertificateException ex) {
			return ControllerUtil.handleKnownNemIDError(model, FailedFlow.NEMID, ErrorType.NEMID_VALIDATION, "nemid.error.notpoces", PageTarget.APP);			
		}
		catch (RuntimeException ex) {
			if (ex.getMessage().contains("challenge")) {
				return ControllerUtil.handleKnownNemIDError(model, FailedFlow.NEMID, ErrorType.NEMID_VALIDATION, "nemid.error.timeout", PageTarget.APP);
			}
			
			// ok, so not challenge related
			log.error("Failure during NemID validation", ex);

			return ControllerUtil.handleError(model, FailedFlow.NEMID, ErrorType.BAD_REQUEST, "Ukendt fejl!", PageTarget.APP);
		}

		HttpSession httpSession = request.getSession();
		Object deviceId = httpSession.getAttribute(ClientSecurityFilter.SESSION_DEVICE_ID);

		if (deviceId == null || !(deviceId instanceof String)) {
			return ControllerUtil.handleError(model, FailedFlow.NEMID, ErrorType.BAD_REQUEST, "No deviceId on session!", PageTarget.APP);
		}

		Client client = clientDao.getByDeviceId((String) deviceId);
		if (client == null) {
			return ControllerUtil.handleError(model, FailedFlow.NEMID, ErrorType.UNKNOWN_CLIENT, "No client with deviceId = " + deviceId, PageTarget.APP);
		}
		
		User user = client.getUser();
		if (user != null) {
			return ControllerUtil.handleError(model, FailedFlow.NEMID, ErrorType.ALREADY_REGISTERED, "Client " + deviceId + " already registered to user " + user.getId(), PageTarget.APP);
		}

		String cpr = result.getCpr();
		String pid = result.getPid();
	            
        user = userService.getByPlainTextSsn(cpr);
        if (user == null) {
        	user = new User();
        	user.setClients(new ArrayList<Client>());		            
            user.setPid(pid);
            user.setSsn(cpr);
        }
        
        user.getClients().add(client);
        client.setUser(user);
        
        userService.save(user);
        
        // remove any local registrations on client
        localClientService.deleteByDeviceId(client.getDeviceId());

		return "redirect:/ui/register2/successPage?status=true&apiKey=" + client.getApiKey() + "&deviceId=" + client.getDeviceId();
	}

	// GET/POST, both are handled here
	@RequestMapping("/ui/register2/successPage")
	public String successPage() {
		return "successPage2";
	}
}
