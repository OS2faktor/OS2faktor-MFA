package dk.digitalidentity.os2faktor.controller.desktop;

import java.util.Date;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.os2faktor.controller.BaseController;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.HardwareToken;
import dk.digitalidentity.os2faktor.dao.model.LocalClient;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.HardwareTokenService;
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
public class HardwareTokenRegisterController extends BaseController {

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private HardwareTokenService hardwareTokenService;

	@Autowired
	private MFATokenManager mfaTokenManager;

	@Autowired
	private LocalClientService localClientService;

	public record HardwareTokenRegistrationRecord(String name, String serial, String code) { }
	
	@GetMapping("/totph")
	public String index(Model model, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request, false);
		if (userOrLoginPage.loginPage != null) {
			String existingRedirectUrl = getExistingRedirectUrl(request);

			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}

			return userOrLoginPage.loginPage;
		}

		model.addAttribute("form", new HardwareTokenRegistrationRecord("", "", ""));
		
		request.getSession().setAttribute(ClientSecurityFilter.SESSION_TYPE, ClientType.TOTPH);
		
		return "hardware_token/initregistration";
	}
	
	@PostMapping("/totph")
	public String initRegister(Model model, @Valid @ModelAttribute("form") HardwareTokenRegistrationRecord form, HttpServletRequest request) {
		UserOrLoginPage userOrLoginPage = authenticateUser(request, false);
		if (userOrLoginPage.loginPage != null) {
			String existingRedirectUrl = getExistingRedirectUrl(request);

			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}

			return userOrLoginPage.loginPage;
		}

		boolean error = validateNameAndSerialNumber(model, form);
		if (error) {
			model.addAttribute("form", form);

			return "hardware_token/initregistration";
		}

		model.addAttribute("form", form);
		
		return "hardware_token/finishregistration";
	}
	
	private boolean validateNameAndSerialNumber(Model model, @Valid HardwareTokenRegistrationRecord form) {
		if (form.name == null || form.name.length() < 3 || form.name.length() > 128) {
			model.addAttribute("nameError", true);
			return true;
		}

		if (form.serial == null || form.serial.length() < 3 || form.serial.length() > 128) {
			model.addAttribute("wrongLength", true);
			return true;
		}

		HardwareToken existingToken = hardwareTokenService.getBySerialnumber(form.serial);
		if (existingToken == null) {
			model.addAttribute("deviceNotFound", true);
			return true;
		}
		else if (existingToken.getRegisteredToCpr() != null || existingToken.getRegisteredToCvr() != null) {
			model.addAttribute("alreadyExists", true);
			return true;
		}

		return false;
	}

	@PostMapping("/totph/register")
	public String endRegister(Model model, @ModelAttribute("form") HardwareTokenRegistrationRecord form, HttpServletRequest request) throws Exception {
		UserOrLoginPage userOrLoginPage = authenticateUser(request, false);
		if (userOrLoginPage.loginPage != null) {
			String existingRedirectUrl = getExistingRedirectUrl(request);

			if (existingRedirectUrl != null) {
				return existingRedirectUrl;
			}
			
			return userOrLoginPage.loginPage;
		}

		// re-validate these fields
		boolean error = validateNameAndSerialNumber(model, form);
		if (error) {
			model.addAttribute("form", form);

			return "hardware_token/initregistration";
		}

		// this is never NULL, because of previous validation
		HardwareToken existingToken = hardwareTokenService.getBySerialnumber(form.serial);

		if (form.code == null || form.code.length() != 6) {
			model.addAttribute("wrongCodeLength", true);
			error = true;
		}
		else {
			// set initial offset
			int offset = (int) existingToken.getOffset();

			// last argument is a span of 21 codes (-300, -270, ..., -30, 0, 30, ..., 270, 300 seconds from current offset)
			// note that this is a special case for registration, as we want to make sure we can register the token, but
			// once registered, the offset will only move +/- 90 seconds from each usage
			OtpVerificationResult otpVerificationResult = mfaTokenManager.verifyTotp(form.code, existingToken.getSecretKey(), offset, 21, Hash.valueOf(existingToken.getHashAlgo()));
			if (!otpVerificationResult.success()) {
				model.addAttribute("wrongCode", true);
				error = true;
			}
			else {
				// if offset differs, update in DB
				int offsetResult = otpVerificationResult.offsetResult();
				if (offsetResult != offset) {
					existingToken.setOffset(offsetResult);

					hardwareTokenService.save(existingToken);
				}				
			}
		}

		if (error) {
			model.addAttribute("form", form);

			return "hardware_token/finishregistration";
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
		
		log.debug("Successfully verified the code.");

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
		client.setName(form.name);
		client.setType(ClientType.TOTPH);
		client.setSecret(existingToken.getSecretKey());
		client.setFailedPinAttempts(0);
		
		// for hand-offs (cvr != null), we do not set User/NSISLevel, these are stored in localClient
		client.setUser(!StringUtils.hasLength(cvr) ? user : null);
		client.setNsisLevel(!StringUtils.hasLength(cvr) ? nsisLevel : NSISLevel.NONE);
		
		if (client.getUser() != null && client.getAssociatedUserTimestamp() == null) {
			client.setAssociatedUserTimestamp(new Date());
		}
		
		client = clientService.save(client);

		existingToken.setRegistered(true);
		existingToken.setClientDeviceId(client.getDeviceId());
		existingToken.setRegisteredToCpr(user.getSsn());
		existingToken.setRegisteredToCvr(cvr);
		hardwareTokenService.save(existingToken);
		
		// store a localClient if this is an external login, which links to the actual user
		if (StringUtils.hasLength(cvr)) {
			LocalClient localClient = new LocalClient();
			localClient.setAdminUserName("OS2faktor Login");
			localClient.setAdminUserUuid("OS2faktor Login");
			localClient.setCvr(cvr);
			localClient.setDeviceId(client.getDeviceId());
			localClient.setNsisLevel(nsisLevel.toString());
			localClient.setSsn(user.getSsn());
			localClient.setTs(new Date());
			
			localClientService.save(localClient);
		}

		String existingRedirectUrl = getExistingRedirectUrl(request);
		if (existingRedirectUrl != null) {
			return existingRedirectUrl + "?result=true&deviceId=" + client.getDeviceId() + "&name=" + client.getName();
		}

		return "hardware_token/registration_success";
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
