package dk.digitalidentity.os2faktor.controller;

import java.util.Base64;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.os2faktor.controller.model.RegisterPayload;
import dk.digitalidentity.os2faktor.controller.model.Registration;
import dk.digitalidentity.os2faktor.controller.model.YubiKeyRegistration;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.PartialClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.LocalClient;
import dk.digitalidentity.os2faktor.dao.model.PartialClient;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.HashingService;
import dk.digitalidentity.os2faktor.service.IdGenerator;
import dk.digitalidentity.os2faktor.service.LocalClientService;
import dk.digitalidentity.os2faktor.service.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class YubiKeyRegisterController extends BaseController {

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private ClientDao clientDao;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PartialClientDao partialClientDao;

	@Autowired
	private LocalClientService localClientService;
	
	@Autowired
	private HashingService hashingService;

	@GetMapping("/ui/yubikey")
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
	
	@PostMapping("/ui/yubikey")
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

		// reload user
		User user = userService.getByEncryptedAndEncodedSsn(userOrLoginPage.user.getSsn());
		if (user == null) {
			log.warn("Authenticated user with pid: " + userOrLoginPage.user.getPid() + " did not exist in database!");

			String existingRedirectUrl = getExistingRedirectUrl(request);
			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}

			return "redirect:/ui/desktop/selfservice/login";
		}

		byte[] uid = idGenerator.getRandomBytes(16);
		byte[] challenge = idGenerator.getRandomBytes(32);
		String challengeB64 = Base64.getEncoder().encodeToString(challenge);
		String uidB64 = Base64.getEncoder().encodeToString(uid);

		PartialClient partialClient = new PartialClient();
		partialClient.setChallenge(challengeB64);
		partialClient.setName(registration.getName());
		partialClient.setType(ClientType.YUBIKEY);
		partialClient.setUser(user);
		partialClient = partialClientDao.save(partialClient);
		
		YubiKeyRegistration form = new YubiKeyRegistration();
		form.setId(partialClient.getId());

		model.addAttribute("name", registration.getName());
		model.addAttribute("uid", uidB64);
		model.addAttribute("challenge", challengeB64);
		model.addAttribute("form", form);
		
		return "yubikey/finishregistration";
	}
	
	@PostMapping("/ui/yubikey/register")
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
			
			return "redirect:/ui/desktop/selfservice/login";			
		}
		
		RegisterPayload payload = form.decode();

		// is this an external handOff?
		String cvr = null;
		String nsisLevel = null;
		Object oCvr = request.getSession().getAttribute(ClientSecurityFilter.SESSION_CVR);
		Object oNsisLevel = request.getSession().getAttribute(ClientSecurityFilter.SESSION_NSIS_LEVEL);
		if ((oCvr instanceof String) && ((String)oCvr).length() > 0 && (oNsisLevel instanceof String) && ((String)oNsisLevel).length() > 0) {
			cvr = (String) oCvr;
			nsisLevel = (String) oNsisLevel;
		}

		// create real client
		Client client = new Client();
		client.setUseCount(0);
		client.setDeviceId(idGenerator.generateDeviceId());
		client.setApiKey(hashingService.encryptAndEncodeString(idGenerator.generateUuid()));
		client.setName(partialClient.getName());
		client.setType(partialClient.getType());
		client.setYubikeyUid(payload.getId());
		client.setYubikeyAttestation(payload.getAttestationObject());
		
		// we do not make a permanent link to the user for external Handoff - this is local-cvr only
		client.setUser(StringUtils.isEmpty(cvr) ? partialClient.getUser() : null);
		if (client.getUser() != null && client.getAssociatedUserTimestamp() == null) {
			client.setAssociatedUserTimestamp(new Date());
		}

		client = clientDao.save(client);	
		
		// store a localClient if this is an external login, which links to the actual user
		if (!StringUtils.isEmpty(cvr)) {
			LocalClient localClient = new LocalClient();
			localClient.setAdminUserName("OS2faktor Login");
			localClient.setAdminUserUuid("OS2faktor Login");
			localClient.setCvr(cvr);
			localClient.setDeviceId(client.getDeviceId());
			localClient.setNsisLevel(nsisLevel);
			localClient.setSsn(partialClient.getUser().getSsn());
			localClient.setTs(new Date());
			
			localClientService.save(localClient);
		}

		String existingRedirectUrl = getExistingRedirectUrl(request);
		if (existingRedirectUrl != null) {
			return existingRedirectUrl;
		}

		// TODO: redirect to an interim success page
		return "redirect:/ui/desktop/selfservice";
	}

	public String getExistingRedirectUrl(HttpServletRequest request) {
		Object redirectUrl = request.getSession().getAttribute(ClientSecurityFilter.SESSION_REDIRECTURL);
		if (redirectUrl != null && redirectUrl instanceof String) {
			request.getSession().removeAttribute(ClientSecurityFilter.SESSION_USER);
			request.getSession().removeAttribute(ClientSecurityFilter.SESSION_REDIRECTURL);
			request.getSession().removeAttribute(ClientSecurityFilter.SESSION_CVR);

			return "redirect:" + (String) redirectUrl;
		}

		return null;
	}
}
