package dk.digitalidentity.os2faktor.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.ui.Model;

import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ControllerUtil {
	public enum PageTarget { DESKTOP, APP };

	// map NemID error codes to a limited set of default error messages
	private static Map<String, String> KNOWN_ERRORS = new HashMap<>();
	
	static {
		KNOWN_ERRORS.put("SRV004", "nemid.error.timeout");
		KNOWN_ERRORS.put("SRV006", "nemid.error.timeout");
		KNOWN_ERRORS.put("LIB002", "nemid.error.timeout");
		KNOWN_ERRORS.put("AUTH", "nemid.error.badaccount");
		KNOWN_ERRORS.put("CAPP", "nemid.error.badaccount");
		KNOWN_ERRORS.put("LOCK", "nemid.error.badaccount");
		KNOWN_ERRORS.put("CAN", "nemid.error.timeout");
		KNOWN_ERRORS.put("OCES", "nemid.error.oces");
	}

	public static String handleError(Model model, FailedFlow flow, ErrorType errorType, String message, PageTarget pageTarget) {
		String uuid = UUID.randomUUID().toString();
		
		model.addAttribute("uuid", uuid);
		model.addAttribute("flow", flow);
		model.addAttribute("errorType", errorType);

		log.warn("uuid=" + uuid + ", flow="+ flow.toString() + ", errorType=" + errorType.toString() + ", message=" + message);

		return (pageTarget.equals(PageTarget.DESKTOP) ? "desktop/" : "") + "error";
	}

	public static boolean knownNemIDError(String errorCode) {
		if (errorCode != null) {
			for (String knownError : KNOWN_ERRORS.keySet()) {
				if (errorCode.startsWith(knownError)) {
					return true;
				}
			}
		}

		return false;
	}

	public static String handleKnownNemIDError(Model model, FailedFlow flow, ErrorType errorType, String errorCode, PageTarget pageTarget) {
		String errorKey = null;
		
		if (errorCode != null) {
			for (String knownError : KNOWN_ERRORS.keySet()) {
				if (errorCode.startsWith(knownError)) {
					errorKey = KNOWN_ERRORS.get(knownError);
					break;
				}
			}
		}

		String uuid = UUID.randomUUID().toString();
		
		model.addAttribute("uuid", uuid);
		model.addAttribute("flow", flow);
		model.addAttribute("errorType", errorType);
		model.addAttribute("nemIDErrorKey", errorKey);
		model.addAttribute("nemIDErrorCode", errorCode);

		log.warn("uuid=" + uuid + ", flow="+ flow.toString() + ", errorType=" + errorType.toString() + ", NemIDErrorCode=" + errorCode);

		return (pageTarget.equals(PageTarget.DESKTOP) ? "desktop/" : "") + "error";
	}
}
