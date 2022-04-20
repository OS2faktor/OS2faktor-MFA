package dk.digitalidentity.os2faktor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DefaultController {

	@Value("${login.enable.nemid:true}")
	private boolean nemIdEnabled;
	
	@Value("${login.enable.unilogin:false}")
	private boolean uniLoginEnabled;
	
	@Value("${login.enable.idp:false}")
	private boolean idpEnabled;
	
	@GetMapping("/")
	public String index(Model model) throws Exception {
		model.addAttribute("nemIdEnabled", nemIdEnabled);
		model.addAttribute("uniLoginEnabled", uniLoginEnabled);
		model.addAttribute("idpEnabled", idpEnabled);
		
		return "index";
	}
}
