package dk.digitalidentity.os2faktor.controller;

import java.nio.charset.Charset;
import java.security.PublicKey;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.yubico.webauthn.data.AttestationObject;
import com.yubico.webauthn.data.AuthenticatorData;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.CollectedClientData;

import dk.digitalidentity.os2faktor.controller.model.LoginPayload;
import dk.digitalidentity.os2faktor.controller.model.LoginPayloadForm;
import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.util.YubiKeyCrypto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class YubiKeyLoginController extends BaseController {

	@Autowired
	private NotificationDao notificationDao;
	
	@Value("${yubikey.origin}")
	private String origin;
	
	@Value("${yubikey.domain}")
	private String domain;

	@GetMapping("/ui/yubikey/login/{pollingKey}")
	public String initLogin(Model model, @PathVariable("pollingKey") String pollingKey, HttpServletRequest request) {
		Notification notification = notificationDao.getByPollingKey(pollingKey);
		if (notification == null) {
			log.warn("Called YubiKey login with unknown pollingKey: " + pollingKey);
			return "yubikey/loginfailed";
		}

		Client client = notification.getClient();
		if (!client.getType().equals(ClientType.YUBIKEY)) {
			log.warn("Called YubiKey login with non-YubiKey client: " + client.getDeviceId());
			return "yubikey/loginfailed";
		}

		model.addAttribute("uid", client.getYubikeyUid());
		model.addAttribute("challenge", notification.getChallenge());
		model.addAttribute("form", new LoginPayloadForm());

		return "yubikey/login";
	}

	@PostMapping("/ui/yubikey/login/{pollingKey}")
	public String endLogin(Model model, @PathVariable("pollingKey") String pollingKey, @ModelAttribute("form") LoginPayloadForm form, HttpServletRequest request) throws Exception {
		Notification notification = notificationDao.getByPollingKey(pollingKey);
		if (notification == null) {
			log.warn("Called YubiKey login with unknown pollingKey: " + pollingKey);
			return "yubikey/loginfailed";
		}

		Client client = notification.getClient();
		if (!client.getType().equals(ClientType.YUBIKEY)) {
			log.warn("Called YubiKey login with non-YubiKey client: " + client.getDeviceId());
			return "yubikey/loginfailed";
		}
		
		LoginPayload payload = form.decode();
		boolean success = validate(payload, client, notification);
		if (!success) {
			return "yubikey/loginfailed";
		}

		notification.setClientAuthenticated(true);
		notificationDao.save(notification);

		return "yubikey/logincompleted";
	}
	
	// TODO: could we use the validation routines from webauthn-server-core directly?
	private boolean validate(LoginPayload payload, Client client, Notification info) {
		try {
			CollectedClientData collectedClientData = payload.decodeClientDataJson();
			AuthenticatorData authenticatorData = payload.decodeAuthenticatorData();
	
			// (step 1) verify that the response comes from the expected client
			if (!payload.getId().equals(client.getYubikeyUid())) {
				log.error("clientId: " + client.getYubikeyUid() + " does not match responseId: " + payload.getId());
				
				return false;
			}
	
			// (step 7) - simple verification of data
			if (!"webauthn.get".equals(collectedClientData.getType())) {
				log.error("clientDataJson.type != webauthn.get");
				
				return false;
			}
	
			// (step 8) - verify that the right challenge was signed
			ByteArray challenge = new ByteArray(Base64.getDecoder().decode(info.getChallenge()));
			ByteArray responseChallenge = collectedClientData.getChallenge();
			if (!challenge.equals(responseChallenge)) {
				log.error("challenges does not match! challenge=" + challenge + ", responseChallenge=" + responseChallenge);
				
				return false;
			}
			
			// (step 9) - check origin is as expected (so they are not responding on some other origin?)
			// TODO: find a better way to check origin :)
			if (!origin.equals(collectedClientData.getOrigin())) {
				log.error("origin does not match! origin=" + origin + ", responseOrigin=" + collectedClientData.getOrigin());
	
				return false;
			}
			
			// (step 11) - compare hashes
		    YubiKeyCrypto crypto = new YubiKeyCrypto();
		    if (!crypto.hash(new ByteArray(domain.getBytes(Charset.forName("UTF-8")))).equals(authenticatorData.getRpIdHash())) {
		    	log.error("hash of document.domain (rpId) and supplied rpIdHash is not equal");
		    	
		    	return false;
		    }
		    
		    // (step 13) - check for user presence
		    if (!authenticatorData.getFlags().UP && ! authenticatorData.getFlags().UV) {
		    	log.error("User presence or verification is required!");
		    	
		    	return false;
	        }
	
	        // (step 16) - verify signature
			byte[] attestationBytes = Base64.getDecoder().decode(client.getYubikeyAttestation());
			AttestationObject attestationObject = new AttestationObject(new ByteArray(attestationBytes));
			ByteArray publicKeyBytes = attestationObject.getAuthenticatorData().getAttestedCredentialData().get().getCredentialPublicKey();
	        PublicKey publicKey = com.yubico.webauthn.WebAuthnCodecs.importCosePublicKey(publicKeyBytes);
	        ByteArray clientDataJsonHash = crypto.hash(payload.clientDataJsonAsByteArray());
	        ByteArray signedBytes = payload.authenticatorDataAsByteArray().concat(clientDataJsonHash);
	        ByteArray signature = payload.signatureAsByteArray();
	
	        if (!crypto.verifySignature(publicKey, signedBytes, signature)) {
	        	log.error("Bad signature");
	        	
	        	return false;
	        }
		    
		    return true;
	    }
	    catch (Exception ex) {
	    	log.error("Exception during validation", ex);
	    	
	    	return false;
	    }
	}
}
