package dk.digitalidentity.os2faktor.controller.desktop;

import java.util.Date;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.os2faktor.controller.BaseController;
import dk.digitalidentity.os2faktor.controller.model.Registration;
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
import dk.digitalidentity.os2faktor.service.MFATokenManager;
import dk.digitalidentity.os2faktor.service.MFATokenManager.OtpVerificationResult;
import dk.digitalidentity.os2faktor.service.UserService;
import dk.digitalidentity.os2faktor.service.totp.Hash;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class AuthenticatorAppRegisterController extends BaseController {

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
	private MFATokenManager mfaTokenManager;

	@GetMapping("/authenticator")
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

		request.getSession().setAttribute(ClientSecurityFilter.SESSION_TYPE, ClientType.TOTP);
		
		return "authenticator_app/initregistration";
	}
	
	public record AuthenticatorAppRegistration(long partialClientId, String qrCode, String qrCodeKey, String mfaCode) { }
	
	@PostMapping("/authenticator")
	public String initRegister(Model model, @Valid @ModelAttribute("form") Registration registration, BindingResult bindingResult, HttpServletRequest request) {
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
			model.addAttribute("form", registration);

			return "authenticator_app/initregistration";
		}
		
		if (registration.getName().length() < 3 || registration.getName().length() > 128) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("form", registration);
			model.addAttribute("nameError", true);

			return "authenticator_app/initregistration";
		}

		// reload user
		User user = userService.getByEncryptedAndEncodedSsn(userOrLoginPage.user.getSsn());
		if (user == null) {
			log.warn("Authenticated user with pid: " + userOrLoginPage.user.getPid() + " did not exist in database!");

			String existingRedirectUrl = getExistingRedirectUrl(request);
			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}

			return "redirect:/";
		}
		
		// Microsoft Authenticator App does not like names with spaces, special characters, numbers, etc, so we
		// do not use the name entered (though it works fine on other Authenticator apps), but instead just
		// hardcode it to "os2faktor".
		String name = "OS2faktor";
		// String name = registration.getName();

		String secret = mfaTokenManager.generateSecretKey();
		String qrCode = mfaTokenManager.getQRCode(secret, name);
		
		if (qrCode == null) {
			log.warn("Failed to generate QR code for new (partial) client with name: " + registration.getName());
			return "redirect:/";
		}
		
		PartialClient partialClient = new PartialClient();
		partialClient.setChallenge(secret);
		partialClient.setName(registration.getName());
		partialClient.setType(ClientType.TOTP);
		partialClient.setUser(user);
		partialClient = partialClientDao.save(partialClient);
		
		model.addAttribute("form", new AuthenticatorAppRegistration(partialClient.getId(), qrCode, secret, ""));
		
		return "authenticator_app/finishregistration";
	}
	
	@PostMapping("/authenticator/register")
	public String endRegister(Model model, @ModelAttribute("form") AuthenticatorAppRegistration form, BindingResult bindingResult, HttpServletRequest request) throws Exception {
		UserOrLoginPage userOrLoginPage = authenticateUser(request, false);
		if (userOrLoginPage.loginPage != null) {
			String existingRedirectUrl = getExistingRedirectUrl(request);

			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}
			
			return userOrLoginPage.loginPage;
		}

		PartialClient partialClient = partialClientDao.findById(form.partialClientId);
		if (partialClient == null) {
			// TODO: setup a better error message for user - we need a failed registration flow page
			log.warn("Authenticated user with pid: " + userOrLoginPage.user.getPid() + " did not have a partial client with id " + form.partialClientId);

			String existingRedirectUrl = getExistingRedirectUrl(request);

			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}
			
			return "redirect:/desktop/selfservice/login";			
		}
		
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("form", form);
			return "authenticator_app/finishregistration";
		}
		
		String secret = partialClient.getChallenge();
		// last argument is a span of 7 codes (-90, -60, -30, 0, 30, 60, 90 seconds from current offset)
		OtpVerificationResult otpVerificationResult = mfaTokenManager.verifyTotp(form.mfaCode, secret, 0, 7, Hash.SHA1);
		if (!otpVerificationResult.success()) {
			model.addAttribute("form", form);
			model.addAttribute("invalidMfa", true);
			return "authenticator_app/finishregistration";
		}
		
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
		client.setSecret(secret);
		client.setFailedPinAttempts(0);
		
		// for hand-offs (cvr != null), we do not set User/NSISLevel, these are stored in localClient
		client.setUser(!StringUtils.hasLength(cvr) ? partialClient.getUser() : null);
		client.setNsisLevel(!StringUtils.hasLength(cvr) ? nsisLevel : NSISLevel.NONE);
		
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

		return "authenticator_app/registration_success";
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
