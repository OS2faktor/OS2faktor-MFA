package dk.digitalidentity.os2faktor.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.os2faktor.config.Constants;
import dk.digitalidentity.os2faktor.service.NemIDService;
import dk.digitalidentity.os2faktor.service.model.PidAndCprOrError;

@Controller
public class NemIdController {

	@Value("${login.enable.nemid:true}")
	private boolean nemIdEnabled;

	@Autowired
	private NemIDService nemIDService;

	@GetMapping("/login/nemid")
	public String loginGet(Model model, HttpServletRequest request) throws Exception {
		if (!nemIdEnabled) {
			return "redirect:/";
		}

		nemIDService.populateModel(model, request);

		return "nemid/login";
	}

	@PostMapping("/login/nemid")
	public String loginPost(Model model, @RequestParam Map<String, String> map, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String responseB64 = map.get("response");
		PidAndCprOrError result = nemIDService.verify(responseB64, request);

		if (result.hasError()) {
			return "password/error";
		}

		request.getSession().setAttribute(Constants.SESSION_SSN, result.getCpr());

		return "redirect:/password/reset";
	}
}
