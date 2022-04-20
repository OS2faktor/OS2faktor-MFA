package dk.digitalidentity.os2faktor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DefaultController {

	@Value("${password.reset.enabled}")
	private boolean enabled;

	@GetMapping("/")
	public String index() throws Exception {
		if (!enabled) {
			throw new Exception("Password reset is not enabled!");
		}

		return "index";
	}
}
