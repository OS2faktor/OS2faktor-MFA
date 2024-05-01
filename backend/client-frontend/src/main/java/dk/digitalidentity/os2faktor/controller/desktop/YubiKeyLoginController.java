package dk.digitalidentity.os2faktor.controller.desktop;

import java.time.LocalDateTime;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;

import dk.digitalidentity.os2faktor.controller.BaseController;
import dk.digitalidentity.os2faktor.controller.model.LoginPayloadForm;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.service.YubiKeyService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class YubiKeyLoginController extends BaseController {
	private static final String SESSION_REDIRECT_URL = "SESSION_REDIRECT_URL";
	private static final String SESSION_REDIRECT_URL_TTS = "SESSION_REDIRECT_URL_TTS";

	@Autowired
	private NotificationDao notificationDao;
	
	@Autowired
	private ClientDao clientDao;

	@Autowired
	private YubiKeyService yubiKeyService;

	@GetMapping("/mfalogin/yubikey/{pollingKey}")
	public String initLogin(Model model, @PathVariable("pollingKey") String pollingKey, @RequestParam(required = false, defaultValue = "", name = "redirectUrl") String redirectUrl, HttpServletRequest request, HttpServletResponse response) {
		Notification notification = notificationDao.findByPollingKey(pollingKey);
		if (notification == null) {
			log.warn("Called YubiKey login with unknown pollingKey: " + pollingKey);
			return "yubikey/loginfailed";
		}

		Client client = notification.getClient();
		if (!client.getType().equals(ClientType.YUBIKEY)) {
			log.warn("Called YubiKey login with non-YubiKey client: " + client.getDeviceId());
			return "yubikey/loginfailed";
		}

		if (StringUtils.hasLength(redirectUrl)) {
			request.getSession().setAttribute(SESSION_REDIRECT_URL, redirectUrl);
			request.getSession().setAttribute(SESSION_REDIRECT_URL_TTS, LocalDateTime.now());
		}

		model.addAttribute("form", new LoginPayloadForm());
		try {
			AssertionRequest assertionRequest = yubiKeyService.startYubiKeyLogin(notification.getClient().getDeviceId());

			// So the backend generated a challenge during the initialization - that challenge is just a dummy-challenge, and is never used.
			// instead we have the client-frontend generate a real challenge (using the yubico framework), which is stored in the notifications
			// table, overwriting the dummy challenge generated by the backend. Later valdiation is done against this new challenge
			notification.setChallenge(assertionRequest.getPublicKeyCredentialRequestOptions().getChallenge().getBase64());
			notificationDao.save(notification);

			model.addAttribute("assertionRequest", assertionRequest.toCredentialsGetJson());
		}
		catch (JsonProcessingException ex) {
			log.error("Parsing assertionRequest (Yubikey Login Options) to JSON failed", ex);
			return "yubikey/loginfailed";
		}

		return "yubikey/login";
	}

	@PostMapping("/mfalogin/yubikey/{pollingKey}")
	public String endLogin(Model model, @PathVariable("pollingKey") String pollingKey, @ModelAttribute("form") LoginPayloadForm form, HttpServletRequest request) throws Exception {
		Notification notification = notificationDao.findByPollingKey(pollingKey);
		if (notification == null) {
			log.warn("Called YubiKey login with unknown pollingKey: " + pollingKey);
			return "yubikey/loginfailed";
		}

		Client client = notification.getClient();
		if (!client.getType().equals(ClientType.YUBIKEY)) {
			log.warn("Called YubiKey login with non-YubiKey client: " + client.getDeviceId());
			return "yubikey/loginfailed";
		}

		AssertionResult loginResult = yubiKeyService.finalizeYubiKeyLogin(notification, form.getResponse());
		if (!loginResult.isSuccess()) {
			return "yubikey/loginfailed";
		}

		notification.setClientAuthenticated(true);
		notification.setClientResponseTimestamp(new Date());
		notificationDao.save(notification);
		
		client.setUseCount(client.getUseCount() + 1);
		clientDao.save(client);

		String redirectUrl = (String) request.getSession().getAttribute(SESSION_REDIRECT_URL);
		LocalDateTime redirectUrlTts = (LocalDateTime) request.getSession().getAttribute(SESSION_REDIRECT_URL_TTS);
		request.getSession().removeAttribute(SESSION_REDIRECT_URL);
		request.getSession().removeAttribute(SESSION_REDIRECT_URL_TTS);
		if (StringUtils.hasLength(redirectUrl) && redirectUrlTts.plusMinutes(10).isAfter(LocalDateTime.now())) {
			return "redirect:" + redirectUrl;
		}
		
		return "yubikey/logincompleted";
	}
}
