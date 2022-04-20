package dk.digitalidentity.os2faktor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SAMLController {

	@Value("${login.enable.idp:false}")
	private boolean idpEnabled;
	
	@Value("${login.enable.unilogin:false}")
	private boolean uniLoginEnabled;
	
	@Value("${saml.metadata.idp.entityid:}")
	private String idpMetadataentityId;
	
	@Value("${saml.metadata.unilogin.entityid:}")
	private String uniLoginMetadataEntityId;

	@GetMapping(path = "/login/discovery")
	public String discovery(Model model) {
		if (idpEnabled) {
			return "redirect:/login/idp";
		}
		else if (uniLoginEnabled) {
			return "redirect:/login/unilogin";
		}

		return "redirect:/";
	}

	@GetMapping("/login/idp")
	public String idpLogin() {
		if (!idpEnabled) {
			return "redirect:/";
		}

		return "redirect:/saml/login?idp=" + idpMetadataentityId;
	}

	@GetMapping("/login/unilogin")
	public String uniLoginLogin() throws Exception {
		if (!uniLoginEnabled) {
			return "redirect:/";
		}

		return "redirect:/saml/login?idp=" + uniLoginMetadataEntityId;
	}
}
