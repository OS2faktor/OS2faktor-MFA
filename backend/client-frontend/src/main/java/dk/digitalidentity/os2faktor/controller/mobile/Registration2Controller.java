package dk.digitalidentity.os2faktor.controller.mobile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.os2faktor.controller.BaseController;
import dk.digitalidentity.os2faktor.controller.ControllerUtil;
import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.controller.model.Registration;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.security.SecurityUtil;
import dk.digitalidentity.os2faktor.service.HashingService;
import dk.digitalidentity.os2faktor.service.IdGenerator;
import dk.digitalidentity.os2faktor.service.LocalClientService;
import dk.digitalidentity.os2faktor.service.PushNotificationSenderService;
import dk.digitalidentity.os2faktor.service.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class Registration2Controller extends BaseController {
	
	@Autowired
	private PushNotificationSenderService snsService;
	
	@Autowired
	private UserService userService;

	@Autowired
	private HashingService hashingService;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private LocalClientService localClientService;

	@Autowired
	private SecurityUtil securityUtil;
	
	@Value("${os2faktor.extensions.chrome.id}")
	private String chromeExtensionId;
	
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
		client.setName(registration.getName());
		client.setToken(sToken);
		client.setType((ClientType) type);
		client.setNsisLevel(NSISLevel.NONE);

		// Generate apiKey
		String apiKey = idGenerator.generateUuid();

		// Encode it and save it on client
		try {
			client.setApiKey(hashingService.encryptAndEncodeString(apiKey));
		} catch (Exception ex) {
			log.error("Failed to encrypt and encode apiKey", ex);
			return ControllerUtil.handleError(model, FailedFlow.REGISTRATION, ErrorType.EXCEPTION, "Failed to encrypt and encode apiKey", PageTarget.APP);
		}

		if (sToken != null) {
			ClientType cType = (ClientType) type;
			if (cType.equals(ClientType.EDGE)) {
				// TODO: do we need to handle EDGE here?
				;
			}
			else {
				try {
					String notificationKey = snsService.createEndpoint(sToken, client.getDeviceId(), (ClientType) type);
					if (StringUtils.hasLength(notificationKey)) {
						client.setNotificationKey(notificationKey);
						
						// see if there are any existing clients with this notification key, and disable them
						// as they are old clients installed on the same device (and now overwritten by the new client)
						List<Client> existingClients = clientService.getByNotificationKey(notificationKey);
						for (Client existingClient : existingClients) {
							// TODO: should probably add the DisabledFalse flag to the query instead for better performance
							if (!existingClient.isDisabled()) {
								existingClient.setDisabled(true);
								clientService.save(existingClient);
							}
						}
					}
				}
				catch (Exception ex) {
					log.error("Failed to register token at AWS SNS", ex);
					
					return ControllerUtil.handleError(model, FailedFlow.REGISTRATION, ErrorType.EXCEPTION, "Failed to register token at AWS SNS", PageTarget.APP);
				}
			}
		}
		
		clientService.save(client);

		return "redirect:/ui/register2/nemid?apiKey=" + apiKey + "&deviceId=" + client.getDeviceId()+"&fromRedirect=true";
	}
	
	@GetMapping("/ui/register2/nemid")
	public String registerNemIdGet(Model model, @RequestParam(name = "deviceId") String deviceId, @RequestParam(name = "apiKey") String apiKey, @RequestParam(required = false, name = "fromRedirect") boolean fromRedirect, HttpServletRequest request) {
		ClientOrErrorPage clientOrErrorPage = authenticateClient(model, FailedFlow.MIT_ID, deviceId, apiKey);
		if (clientOrErrorPage.errorPage != null) {
			return clientOrErrorPage.errorPage;
		}

		// the called method always returns a client - it cannot return a user
		Client client = clientOrErrorPage.client;		
        User user = client.getUser();
        if (user != null) {
        	return ControllerUtil.handleError(model, FailedFlow.MIT_ID, ErrorType.ALREADY_REGISTERED, "Client " + deviceId + " already registered to user " + user.getId(), PageTarget.APP);
        }

        // detect old/un-patched windows clients (default emulation mode is IE7, it needs to run in IE11 mode)
        if (client.getType().equals(ClientType.WINDOWS)) {
	        String userAgent = request.getHeader("user-agent");
	        if (userAgent != null && userAgent.contains("MSIE 7.0")) {
	        	return "upgradewin";
	        }
        }
        
		request.getSession().setAttribute(ClientSecurityFilter.SESSION_DEVICE_ID, deviceId);
		request.getSession().setAttribute(ClientSecurityFilter.SESSION_API_KEY, apiKey);
		
		model.addAttribute("apiKey", apiKey);
		model.addAttribute("deviceId", deviceId);
		model.addAttribute("fromRedirect", fromRedirect);
		
		return "nemid2";
	}
	
	// not with /ui prefix, to make sure SAML authentication is performed
	@GetMapping(value = "/auth/register2/nemid/complete")
	public String login(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Validate that the user was successfully authenticated
		if (!securityUtil.isAuthenticated()) {
			return ControllerUtil.handleError(model, FailedFlow.MIT_ID, ErrorType.MITID_VALIDATION, "Authentification with MitID failed", PageTarget.APP);
		}

		// Get and validate deviceId from session
		HttpSession httpSession = request.getSession();
		Object deviceId = httpSession.getAttribute(ClientSecurityFilter.SESSION_DEVICE_ID);

		if (deviceId == null || !(deviceId instanceof String)) {
			return ControllerUtil.handleError(model, FailedFlow.MIT_ID, ErrorType.BAD_REQUEST, "No deviceId on session!", PageTarget.APP);
		}

		// Get and validate apiKey
		Object apiKey = httpSession.getAttribute(ClientSecurityFilter.SESSION_API_KEY);
		if (apiKey == null || !(apiKey instanceof String)) {
			return ControllerUtil.handleError(model, FailedFlow.MIT_ID, ErrorType.BAD_REQUEST, "No apiKey on session!", PageTarget.APP);
		}

		// Get Client by deviceId
		Client client = clientService.getByDeviceId((String) deviceId);
		if (client == null) {
			return ControllerUtil.handleError(model, FailedFlow.MIT_ID, ErrorType.UNKNOWN_CLIENT, "No client with deviceId = " + deviceId, PageTarget.APP);
		}

		// Check if a user is already associated with the client, if so show an error
		User user = client.getUser();
		if (user != null) {
			return ControllerUtil.handleError(model, FailedFlow.MIT_ID, ErrorType.ALREADY_REGISTERED, "Client " + deviceId + " already registered to user " + user.getId(), PageTarget.APP);
		}

		String cpr = securityUtil.getCpr();
		String pid = securityUtil.getTokenUser().getUsername();

		if (!StringUtils.hasLength(cpr)) {
			return ControllerUtil.handleError(model, FailedFlow.MIT_ID, ErrorType.BAD_REQUEST, "No cpr on session!", PageTarget.APP);
		}
		
		// If the user does not exist, create the user
		user = userService.getByPlainTextSsn(cpr);
		if (user == null) {
			// fallback to lookup by encoded cpr, as the PID lookup might have failed, and
			// we used our database for lookup
			user = userService.getByEncryptedAndEncodedSsn(cpr);
		}

        if (user == null) {
        	user = new User();
        	user.setClients(new ArrayList<Client>());		            
            user.setPid(pid);
            user.setSsn(cpr);
        }

		// Associate the user with the device
        user.getClients().add(client);
        client.setUser(user);
        client.setAssociatedUserTimestamp(new Date());
        client.setNsisLevel(getNsisLevel(request));
        
        userService.save(user);
        
        // Remove any local registrations on client
        localClientService.deleteByDeviceId(client.getDeviceId());

        String nsisWarning = "";
        if (NSISLevel.NONE.equals(client.getNsisLevel())) {
        	nsisWarning = "&nsisWarning=true";
        }
        
		return "redirect:/ui/register2/successPage?status=true&apiKey=" + apiKey + "&deviceId=" + client.getDeviceId() + nsisWarning;
	}

	// GET/POST, both are handled here
	@RequestMapping("/ui/register2/successPage")
	public String successPage(Model model, @RequestParam(value = "nsisWarning", required = false, defaultValue = "false") String nsisWarning) {
		model.addAttribute("chromeExtensionId", chromeExtensionId);
		model.addAttribute("nsisWarning", ("true".equals(nsisWarning)));
		
		return "successPage2";
	}
}
