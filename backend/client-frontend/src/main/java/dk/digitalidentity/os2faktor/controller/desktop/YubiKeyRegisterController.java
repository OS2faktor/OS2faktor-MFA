package dk.digitalidentity.os2faktor.controller.desktop;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

import dk.digitalidentity.os2faktor.controller.BaseController;
import dk.digitalidentity.os2faktor.controller.model.Registration;
import dk.digitalidentity.os2faktor.controller.model.YubiKeyRegistration;
import dk.digitalidentity.os2faktor.dao.PartialClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.LocalClient;
import dk.digitalidentity.os2faktor.dao.model.PartialClient;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.HashingService;
import dk.digitalidentity.os2faktor.service.IdGenerator;
import dk.digitalidentity.os2faktor.service.LocalClientService;
import dk.digitalidentity.os2faktor.service.UserService;
import dk.digitalidentity.os2faktor.service.YubiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class YubiKeyRegisterController extends BaseController {

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PartialClientDao partialClientDao;

	@Autowired
	private LocalClientService localClientService;
	
	@Autowired
	private HashingService hashingService;

	@Autowired
	private YubiKeyService yubiKeyService;

	@GetMapping("/yubikey")
	public String index(Model model, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request, false);
		if (userOrLoginPage.loginPage != null) {
			String existingRedirectUrl = getExistingRedirectUrl(request);

			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}

			return userOrLoginPage.loginPage;
		}

		model.addAttribute("form", new Registration());

		request.getSession().setAttribute(ClientSecurityFilter.SESSION_TYPE, ClientType.YUBIKEY);
		
		return "yubikey/initregistration";
	}
	
	@PostMapping("/yubikey")
	public String initRegister(Model model, @ModelAttribute("form") Registration registration, BindingResult bindingResult, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request, false);
		if (userOrLoginPage.loginPage != null) {
			String existingRedirectUrl = getExistingRedirectUrl(request);

			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}

			return userOrLoginPage.loginPage;
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("registration", registration);

			return "yubikey/initregistration";
		}

		if (registration.getName().length() < 3 || registration.getName().length() > 128) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("registration", registration);
			model.addAttribute("nameError", true);

			return "yubikey/initregistration";
		}

		// reload user
		User user = userService.getByEncryptedAndEncodedSsn(userOrLoginPage.user.getSsn());
		if (user == null) {
			log.warn("Authenticated user with pid: " + userOrLoginPage.user.getPid() + " did not exist in database!");

			String existingRedirectUrl = getExistingRedirectUrl(request);
			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}

			return "redirect:/desktop/selfservice/login";
		}

		try {
			// Start yubikey registration
			PublicKeyCredentialCreationOptions creationOptions = yubiKeyService.startYubiKeyRegistration(registration.getName());
			model.addAttribute("creationOptions", creationOptions.toCredentialsCreateJson());

			// Create partial client for later
			PartialClient partialClient = new PartialClient();
			partialClient.setChallenge(creationOptions.getChallenge().getBase64());
			partialClient.setName(registration.getName());
			partialClient.setType(ClientType.YUBIKEY);
			partialClient.setUser(user);
			partialClient = partialClientDao.save(partialClient);

			// Include partial client info in the model
			YubiKeyRegistration form = new YubiKeyRegistration();
			form.setId(partialClient.getId());
			model.addAttribute("form", form);

			return "yubikey/finishregistration";
		}
		catch (JsonProcessingException ex) {
			model.addAttribute(ex);
			model.addAttribute("registration", registration);
			model.addAttribute("nameError", false);

			log.error("Parsing assertionRequest (Yubikey Login Options) to JSON failed", ex);
			return "yubikey/initregistration";
		}
	}
	
	@PostMapping("/yubikey/register")
	public String endRegister(Model model, @ModelAttribute("form") YubiKeyRegistration form, HttpServletRequest request) throws Exception {
		UserOrLoginPage userOrLoginPage = authenticateUser(request, false);
		if (userOrLoginPage.loginPage != null) {
			String existingRedirectUrl = getExistingRedirectUrl(request);

			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}

			return userOrLoginPage.loginPage;
		}

		PartialClient partialClient = partialClientDao.getById(form.getId());
		if (partialClient == null) {
			// TODO: setup a better error message for user - we need a failed registration flow page
			log.warn("Authenticated user with pid: " + userOrLoginPage.user.getPid() + " did not have a partial client with id " + form.getId());

			String existingRedirectUrl = getExistingRedirectUrl(request);

			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}

			return "redirect:/desktop/selfservice/login";
		}

		YubiKeyService.YubikeyRegistrationDTO registration = yubiKeyService.finalizeYubikeyRegistration(partialClient, form.getResponse());

		NSISLevel nsisLevel = getNsisLevel(request);
		String cvr = null;

		Object oCvr = request.getSession().getAttribute(ClientSecurityFilter.SESSION_CVR);
		if (oCvr != null && oCvr instanceof String && ((String)oCvr).length() > 0) {
			cvr = (String) oCvr;
		}

		// create real client
		Client client = new Client();
		client.setUseCount(0);
		client.setDeviceId(idGenerator.generateDeviceId());
		client.setApiKey(hashingService.encryptAndEncodeString(idGenerator.generateUuid()));
		client.setName(partialClient.getName());
		client.setType(partialClient.getType());
		client.setYubikeyUid(registration.result().getKeyId().getId().getBase64());
		client.setYubikeyAttestation(registration.response().getResponse().getAttestationObject().getBase64());

		// for hand-offs (cvr != null), we do not set User/NSISLevel, these are stored in localClient
		client.setUser(!StringUtils.hasLength(cvr) ? partialClient.getUser() : null);
		client.setNsisLevel(!StringUtils.hasLength(cvr) ? nsisLevel : NSISLevel.NONE);

		// we do not make a permanent link to the user for external Handoff - this is local-cvr only
		client.setUser(!StringUtils.hasLength(cvr) ? partialClient.getUser() : null);
		if (client.getUser() != null && client.getAssociatedUserTimestamp() == null) {
			client.setAssociatedUserTimestamp(new Date());
		}

		client = clientService.save(client);

		// store a localClient if this is an external login, which links to the actual user
		if (StringUtils.hasLength(cvr)) {
			LocalClient localClient = new LocalClient();
			localClient.setAdminUserName("OS2faktor Login");
			localClient.setAdminUserUuid("OS2faktor Login");
			localClient.setCvr(cvr);
			localClient.setDeviceId(client.getDeviceId());
			localClient.setNsisLevel(nsisLevel.toString());
			localClient.setSsn(partialClient.getUser().getSsn());
			localClient.setTs(new Date());

			localClientService.save(localClient);
		}

		String existingRedirectUrl = getExistingRedirectUrl(request);
		if (existingRedirectUrl != null) {
			return existingRedirectUrl + "?result=true&deviceId=" + client.getDeviceId() + "&name=" + client.getName();
		}

		// TODO: redirect to an interim success page
		return "redirect:/";
	}

	public String getExistingRedirectUrl(HttpServletRequest request) {
		Object redirectUrl = request.getSession().getAttribute(ClientSecurityFilter.SESSION_REDIRECTURL);
		if (redirectUrl != null && redirectUrl instanceof String) {
			request.getSession().removeAttribute(ClientSecurityFilter.SESSION_USER);
			request.getSession().removeAttribute(ClientSecurityFilter.SESSION_REDIRECTURL);
			request.getSession().removeAttribute(ClientSecurityFilter.SESSION_REDIRECTURL_TTS);
			request.getSession().removeAttribute(ClientSecurityFilter.SESSION_CVR);

			return "redirect:" + (String) redirectUrl;
		}

		return null;
	}
}
