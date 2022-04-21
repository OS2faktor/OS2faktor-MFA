package dk.digitalidentity.os2faktor.controller;

import java.util.UUID;

import org.springframework.ui.Model;

import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ControllerUtil {
	public enum PageTarget { DESKTOP, APP };

	public static String handleError(Model model, FailedFlow flow, ErrorType errorType, String message, PageTarget pageTarget) {
		String uuid = UUID.randomUUID().toString();
		
		model.addAttribute("uuid", uuid);
		model.addAttribute("flow", flow);
		model.addAttribute("errorType", errorType);

		log.warn("uuid=" + uuid + ", flow="+ flow.toString() + ", errorType=" + errorType.toString() + ", message=" + message);

		return (pageTarget.equals(PageTarget.DESKTOP) ? "desktop/" : "") + "error";
	}
}
