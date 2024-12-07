package dk.digitalidentity.os2faktor.controller.desktop;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

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
import dk.digitalidentity.os2faktor.dao.model.HardwareToken;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;
import dk.digitalidentity.os2faktor.service.HardwareTokenService;
import dk.digitalidentity.os2faktor.service.MFATokenManager;
import dk.digitalidentity.os2faktor.service.MFATokenManager.OtpVerificationResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class AuthenticatorAppLoginController extends BaseController {

	@Autowired
	private NotificationDao notificationDao;

	@Autowired
	private MFATokenManager mfaTokenManager;
	
	@Autowired
	private HardwareTokenService hardwareTokenService;

	public record AuthenticatorForm (String mfaCode, boolean reject) { }

	// This handles both TOTPH and TOTP logic (the registration flows differ, but not login flows)
	
	@GetMapping("/mfalogin/authenticator/{pollingKey}")
	public String initLogin(Model model, @PathVariable("pollingKey") String pollingKey, @RequestParam(required = false, defaultValue = "", name = "redirectUrl") String redirectUrl, HttpServletRequest request) {
		Notification notification = notificationDao.findByPollingKey(pollingKey);
		if (notification == null) {
			log.warn("Called TOTP or TOTPH login with unknown pollingKey: " + pollingKey);
			return "authenticator_app/loginfailed";
		}

		Client client = notification.getClient();
		model.addAttribute("clientType", client.getType().toString());
		if (!client.getType().equals(ClientType.TOTP) && !client.getType().equals(ClientType.TOTPH)) {
			log.warn("Called TOTP or TOTPH login with non-TOTP client: " + client.getDeviceId());
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
			request.getSession().setAttribute(ClientSecurityFilter.SESSION_REDIRECTURL, redirectUrl);
			request.getSession().setAttribute(ClientSecurityFilter.SESSION_REDIRECTURL_TTS, LocalDateTime.now());
		}
		
		model.addAttribute("form", new AuthenticatorForm("", false));

		return "authenticator_app/login";
	}

	@PostMapping("/mfalogin/authenticator/{pollingKey}")
	public String endLogin(Model model, @PathVariable("pollingKey") String pollingKey, @ModelAttribute("form") AuthenticatorForm form, BindingResult bindingResult, HttpServletRequest request) throws Exception {
		Notification notification = notificationDao.findByPollingKey(pollingKey);
		if (notification == null) {
			log.warn("Called TOTP or TOTPH login with unknown pollingKey: " + pollingKey);
			return "authenticator_app/loginfailed";
		}

		Client client = notification.getClient();
		model.addAttribute("clientType", client.getType().toString());
		if (!client.getType().equals(ClientType.TOTP) && !client.getType().equals(ClientType.TOTPH)) {
			log.warn("Called TOTP or TOTPH login with non-TOTP client: " + client.getDeviceId());
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
			
			String redirectUrl = (String) request.getSession().getAttribute(ClientSecurityFilter.SESSION_REDIRECTURL);
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
		
		// set initialOffset parameter if TOTPH
		int offset = 0;
		HardwareToken token = null;
		if (client.getType().equals(ClientType.TOTPH)) {
			token = hardwareTokenService.getByClient(client.getDeviceId());
			if (token != null) {
				offset = (int) token.getOffset();
			}
		}

		// last argument is a span of 7 codes (-90, -60, -30, 0, 30, 60, 90 seconds from current offset)
		OtpVerificationResult otpVerificationResult = mfaTokenManager.verifyTotp(form.mfaCode, client.getSecret(), offset, 7);
		if (!otpVerificationResult.success()) {
			client.setFailedPinAttempts(client.getFailedPinAttempts() + 1);
			clientService.save(client);
			
			model.addAttribute("invalidMfa", true);
			model.addAttribute("form", form);
			
			return "authenticator_app/login";
		}

		// success - so update offset on client (if TOTPH)
		if (token != null) {
			int offsetResult = otpVerificationResult.offsetResult();
			if (offsetResult != offset) {
				token.setOffset(offsetResult);

				hardwareTokenService.save(token);
			}
		}

		client.setFailedPinAttempts(0);
		client.setUseCount(client.getUseCount() + 1);
		clientService.save(client);
		
		notification.setClientAuthenticated(true);
		notification.setClientResponseTimestamp(new Date());
		notificationDao.save(notification);

		String redirectUrl = (String) request.getSession().getAttribute(ClientSecurityFilter.SESSION_REDIRECTURL);
		LocalDateTime redirectUrlTts = (LocalDateTime) request.getSession().getAttribute(ClientSecurityFilter.SESSION_REDIRECTURL_TTS);
		request.getSession().removeAttribute(ClientSecurityFilter.SESSION_REDIRECTURL);
		request.getSession().removeAttribute(ClientSecurityFilter.SESSION_REDIRECTURL_TTS);
		
		// TODO: how can redirectUrlTts be null if redirectUrl has length? - do we ever save one but not the other?
		if (StringUtils.hasLength(redirectUrl) && (redirectUrlTts == null || redirectUrlTts.plusMinutes(10).isAfter(LocalDateTime.now()))) {
			return "redirect:" + redirectUrl;
		}
		
		return "authenticator_app/logincompleted";
	}
}
