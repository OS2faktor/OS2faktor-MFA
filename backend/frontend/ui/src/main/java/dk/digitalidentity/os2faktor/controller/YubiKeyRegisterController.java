package dk.digitalidentity.os2faktor.controller;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import dk.digitalidentity.os2faktor.dao.model.PartialClient;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.IdGenerator;
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

	@GetMapping("/ui/yubikey")
	public String index(Model model, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request);
		if (userOrLoginPage.loginPage != null) {
			return userOrLoginPage.loginPage;
		}

		model.addAttribute("form", new Registration());

		request.getSession().setAttribute(ClientSecurityFilter.SESSION_TYPE, ClientType.YUBIKEY);
		
		return "yubikey/initregistration";
	}
	
	@PostMapping("/ui/yubikey")
	public String initRegister(Model model, @ModelAttribute("form") Registration registration, BindingResult bindingResult, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request);
		if (userOrLoginPage.loginPage != null) {
			return userOrLoginPage.loginPage;
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("registration", registration);

			return "yubikey/initregistration";
		}

		// reload user
		User user = userService.getByPid(userOrLoginPage.user.getPid());
		if (user == null) {
			log.warn("Authenticated user with pid: " + userOrLoginPage.user.getPid() + " did not exist in database!");

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
		UserOrLoginPage userOrLoginPage = authenticateUser(request);
		if (userOrLoginPage.loginPage != null) {
			return userOrLoginPage.loginPage;
		}

		PartialClient partialClient = partialClientDao.getById(form.getId());
		if (partialClient == null) {
			// TODO: setup a better error message for user - we need a failed registration flow page
			log.warn("Authenticated user with pid: " + userOrLoginPage.user.getPid() + " did not have a partial client with id " + form.getId());

			return "redirect:/ui/desktop/selfservice/login";			
		}
		
		RegisterPayload payload = form.decode();
		
		// TODO: do validation I guess... verify challenge and stuff like that (not super important, but still)
		//partialClient.getChallenge()

		// create real client
		Client client = new Client();
		client.setUseCount(0);
		client.setDeviceId(idGenerator.generateDeviceId());
		client.setApiKey(idGenerator.generateUuid());
		client.setName(partialClient.getName());
		client.setType(partialClient.getType());
		client.setUser(partialClient.getUser());
		client.setYubikeyUid(payload.getId());
		client.setYubikeyAttestation(payload.getAttestationObject());
		clientDao.save(client);

		// TODO: redirect to an interim success page
		return "redirect:/ui/desktop/selfservice";
	}
}
