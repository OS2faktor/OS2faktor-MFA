package dk.digitalidentity.os2faktor.controller.desktop;

import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.os2faktor.controller.BaseController;
import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.service.ClientService;
import dk.digitalidentity.os2faktor.service.MFATokenManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class AuthenticatorAppLoginController extends BaseController {
	private static final String SESSION_REDIRECT_URL = "SESSION_REDIRECT_URL";

	@Autowired
	private NotificationDao notificationDao;

	@Autowired
	private MFATokenManager mfaTokenManager;
	
	@Autowired
	private ClientService clientService;

	public record AuthenticatorForm (String mfaCode, boolean reject) { }

	@GetMapping("/mfalogin/authenticator/{pollingKey}")
	public String initLogin(Model model, @PathVariable("pollingKey") String pollingKey, @RequestParam(required = false, defaultValue = "", name = "redirectUrl") String redirectUrl, HttpServletRequest request) {
		Notification notification = notificationDao.getByPollingKey(pollingKey);
		if (notification == null) {
			log.warn("Called TOTP login with unknown pollingKey: " + pollingKey);
			return "authenticator_app/loginfailed";
		}

		Client client = notification.getClient();
		if (!client.getType().equals(ClientType.TOTP)) {
			log.warn("Called TOTP login with non-TOTP client: " + client.getDeviceId());
			return "authenticator_app/loginfailed";
		}
		
		// handle if client is locked
		if (client.isLocked()) {
			log.warn("Client with deviceId: " + client.getDeviceId() + " tried to accept a challenge while being locked out." );
			model.addAttribute("lockedOut", true);

			return "authenticator_app/loginfailed";
		}
		
		// handle if client should be locked
		if (client.getFailedPinAttempts() >= 5) {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MINUTE, 5);
			Date lockedUntil = c.getTime();

			client.setFailedPinAttempts(0);
			client.setLockedUntil(lockedUntil);
			client.setLocked(true);
			
			clientService.save(client);
			
			log.warn("Client was locked due to too many wrong mfa codes: " + client.getDeviceId());
			model.addAttribute("lockedOut", true);
			return "authenticator_app/loginfailed";
		}

		if (StringUtils.hasLength(redirectUrl)) {
			request.getSession().setAttribute(SESSION_REDIRECT_URL, redirectUrl);
		}
		
		model.addAttribute("form", new AuthenticatorForm("", false));

		return "authenticator_app/login";
	}

	@PostMapping("/mfalogin/authenticator/{pollingKey}")
	public String endLogin(Model model, @PathVariable("pollingKey") String pollingKey, @ModelAttribute("form") AuthenticatorForm form, BindingResult bindingResult, HttpServletRequest request) throws Exception {
		Notification notification = notificationDao.getByPollingKey(pollingKey);
		if (notification == null) {
			log.warn("Called TOTP login with unknown pollingKey: " + pollingKey);
			return "authenticator_app/loginfailed";
		}

		Client client = notification.getClient();
		if (!client.getType().equals(ClientType.TOTP)) {
			log.warn("Called TOTP login with non-TOTP client: " + client.getDeviceId());
			return "authenticator_app/loginfailed";
		}
		
		// handle if client is locked
		if (client.isLocked()) {
			log.warn("Client with deviceId: " + client.getDeviceId() + " tried to accept a challenge while being locked out." );
			model.addAttribute("lockedOut", true);
			return "authenticator_app/loginfailed";
		}
		
		// handle reject
		if (form.reject) {
			notification.setClientRejected(true);
			notification.setClientResponseTimestamp(new Date());
			notificationDao.save(notification);
			
			String redirectUrl = (String) request.getSession().getAttribute(SESSION_REDIRECT_URL);
			if (StringUtils.hasLength(redirectUrl)) {
				return "redirect:" + redirectUrl;
			}
			
			return "authenticator_app/reject_completed";
		}
		
		// handle if client should be locked
		if (client.getFailedPinAttempts() >= 5) {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MINUTE, 5);
			Date lockedUntil = c.getTime();

			client.setFailedPinAttempts(0);
			client.setLockedUntil(lockedUntil);
			client.setLocked(true);
			
			clientService.save(client);
			
			log.warn("Client was locked due to too many wrong mfa codes: " + client.getDeviceId());
			model.addAttribute("lockedOut", true);
			return "authenticator_app/loginfailed";
		}
		
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("form", form);
			return "authenticator_app/login";
		}
		
		if (!mfaTokenManager.verifyTotp(form.mfaCode, client.getSecret())) {
			client.setFailedPinAttempts(client.getFailedPinAttempts() + 1);
			clientService.save(client);
			
			model.addAttribute("invalidMfa", true);
			model.addAttribute("form", form);
			return "authenticator_app/login";
		}

		client.setFailedPinAttempts(0);
		clientService.save(client);
		
		notification.setClientAuthenticated(true);
		notification.setClientResponseTimestamp(new Date());
		notificationDao.save(notification);

		String redirectUrl = (String) request.getSession().getAttribute(SESSION_REDIRECT_URL);
		if (StringUtils.hasLength(redirectUrl)) {
			return "redirect:" + redirectUrl;
		}
		
		return "authenticator_app/logincompleted";
	}
}
