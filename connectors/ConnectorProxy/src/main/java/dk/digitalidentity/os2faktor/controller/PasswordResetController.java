package dk.digitalidentity.os2faktor.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.os2faktor.controller.model.NewPasswordForm;
import dk.digitalidentity.os2faktor.controller.validators.PasswordValidator;
import dk.digitalidentity.os2faktor.service.LdapService;
import dk.digitalidentity.os2faktor.service.NemIDService;
import dk.digitalidentity.os2faktor.service.model.PidAndCprOrError;
import dk.digitalidentity.os2faktor.service.model.UsernameAndPassword;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class PasswordResetController {
	private static final String SESSION_SSN = "SESSION_SSN";
	private static final String SESSION_SAMACCOUNTNAME = "SESSION_SAMACCOUNTNAME";
	private static final String SESSION_SAMACCOUNTNAMES = "SESSION_SAMACCOUNTNAMES";
	private static final String SESSION_DESKTOP = "SESSION_DESKTOP";

	@Autowired
	private LdapService ldapService;

	@Autowired
	private NemIDService nemIDService;

	@Autowired
	private PasswordValidator passwordValidator;

	@Value("${password.reset.enabled}")
	private boolean enabled;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(passwordValidator);
	}

	@GetMapping("/password/resetpassword")
	public String resetDesktopGet(HttpServletRequest request) throws Exception {
		if (!enabled) {
			throw new Exception("Password reset is not enabled!");
		}

		request.getSession().setAttribute(SESSION_DESKTOP, "true");

		return "redirect:/password/reset";
	}
	
	@GetMapping("/password/reset")
	public String resetGet(Model model, HttpServletRequest request) throws Exception {
		if (!enabled) {
			throw new Exception("Password reset is not enabled!");
		}

		String ssn = authenticateUser(request);
		if (ssn == null) {
			return "redirect:/password/reset/login";
		}

		List<String> sAMAccountNames = ldapService.getSAMAccountNames(ssn);
		if (sAMAccountNames.size() == 0) {
			log.error("No user in Active Directory with ssn=" + ldapService.maskSsn(ssn));

			return "password/error";
		}
		else if (sAMAccountNames.size() == 1) {
			try {
				String sAMAccountName = sAMAccountNames.get(0);
				if (ldapService.isAllowedToChangePassword(sAMAccountName)) {
					request.getSession().setAttribute(SESSION_SAMACCOUNTNAME, sAMAccountName);
					model.addAttribute("newPasswordForm", new NewPasswordForm());

					return "password/newPassword";
				}
				else {
					return "password/nopasswordchange";
				}
			}
			catch (Exception ex) {
				log.error("Failed to validate if user can change password with ssn=" + ldapService.maskSsn(ssn), ex);
			}

			return "password/error";
		}
		else {
			request.getSession().setAttribute(SESSION_SAMACCOUNTNAMES, sAMAccountNames);
			model.addAttribute("users", sAMAccountNames);

			return "password/pickuser";
		}
	}
	
	@GetMapping("/password/reset/{sAMAccountName}")
	public String resetGetWithSAMAccountName(Model model, @PathVariable("sAMAccountName") String sAMAccountName, HttpServletRequest request) throws Exception {
		if (!enabled) {
			throw new Exception("Password reset is not enabled!");
		}
		
		@SuppressWarnings("unchecked")
		List<String> sAMAccountNames = (List<String>) request.getSession().getAttribute(SESSION_SAMACCOUNTNAMES);
		if (!sAMAccountNames.contains(sAMAccountName)) {
			log.error("User picked a sAMAccountName that his/her ssn is not associated with...");
			return "password/error";
		}

		try {
			if (ldapService.isAllowedToChangePassword(sAMAccountName)) {
				request.getSession().setAttribute(SESSION_SAMACCOUNTNAME, sAMAccountName);
				model.addAttribute("newPasswordForm", new NewPasswordForm());

				return "password/newPassword";
			}
			else {
				return "password/nopasswordchange";
			}
		}
		catch (Exception ex) {
			log.error("Failed to validate if user can change password with sAMAccountName=" + sAMAccountName, ex);
		}

		return "password/error";
	}

	@RequestMapping(path = "/password/reset", method = RequestMethod.POST, consumes= "application/x-www-form-urlencoded;charset=UTF-8")
	public String resetPassword(Model model, @Valid @ModelAttribute("newPasswordForm") NewPasswordForm newPasswordForm , BindingResult bindingResult, HttpServletRequest request) throws Exception {
		if (!enabled) {
			throw new Exception("Password reset is not enabled!");
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute("newPasswordForm", newPasswordForm);
			return "password/newPassword";
		}

		String sAMAccountName = (String) request.getSession().getAttribute(SESSION_SAMACCOUNTNAME);
		if (sAMAccountName == null) {
			return "redirect:/password/reset/login";
		}

		try {
			UsernameAndPassword result = ldapService.resetPassword(sAMAccountName, newPasswordForm.getNewPassword());

			boolean desktop = false;
			Object o = request.getSession().getAttribute(SESSION_DESKTOP);
			if (o != null && o instanceof String) {
				if ("true".equals((String) o)) {
					desktop = true;
				}
			}
			
			if (!desktop) {
				String password = result.getPassword();
				String username = result.getUsername();
	
				model.addAttribute("password", password);
				model.addAttribute("username", username);
			}
			
			log.info("Password changed for '" + result.getUsername() + "'");
			
			return "password/success";
		}
		catch (Exception ex) {
			log.error("Failed to change password for user with samAccountName=" + sAMAccountName, ex);
		}

		model.addAttribute("message", "Failed to change password.");
		return "password/error";
	}

	@GetMapping("/password/reset/login")
	public String loginGet(Model model, HttpServletRequest request) throws Exception {
		if (!enabled) {
			throw new Exception("Password reset is not enabled!");
		}

		nemIDService.populateModel(model, request);

		return "password/login";
	}

	@PostMapping("/password/reset/login")
	public String loginPost(Model model, @RequestParam Map<String, String> map, HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (!enabled) {
			throw new Exception("Password reset is not enabled!");
		}

		String responseB64 = map.get("response");
		PidAndCprOrError result = nemIDService.verify(responseB64, request);

		if (result.hasError()) {
			return "password/error";
		}

		request.getSession().setAttribute(SESSION_SSN, result.getCpr());

		return "redirect:/password/reset";
	}

	private String authenticateUser(HttpServletRequest request) {
		Object o = request.getSession().getAttribute(SESSION_SSN);
		if (o == null || !(o instanceof String)) {
			return null;
		}

		return (String) o;
	}
}
