package dk.digitalidentity.os2faktor.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.os2faktor.config.Constants;
import dk.digitalidentity.os2faktor.controller.model.NewPasswordForm;
import dk.digitalidentity.os2faktor.controller.validators.PasswordValidator;
import dk.digitalidentity.os2faktor.security.SecurityUtil;
import dk.digitalidentity.os2faktor.service.LdapService;
import dk.digitalidentity.os2faktor.service.model.UsernameAndPassword;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class PasswordResetController {

	@Autowired
	private LdapService ldapService;

	@Autowired
	private PasswordValidator passwordValidator;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(passwordValidator);
	}
	
	@GetMapping("/password/reset")
	public String resetGet(Model model, HttpServletRequest request) throws Exception {
		String sAMAccountName = SecurityUtil.getSAMAccountName();
		if (!StringUtils.isEmpty(sAMAccountName)) {
			return "redirect:/password/reset/" + sAMAccountName;
		}

		String ssn = SecurityUtil.getSsn();
		if (StringUtils.isEmpty(ssn)) {
			return "redirect:/";
		}

		List<String> sAMAccountNames = ldapService.getSAMAccountNames(ssn);
		if (sAMAccountNames.size() == 0) {
			log.error("No user in Active Directory with ssn=" + ldapService.maskSsn(ssn));

			return "password/error";
		}
		else if (sAMAccountNames.size() == 1) {
			sAMAccountName = sAMAccountNames.get(0);

			try {
				if (ldapService.isAllowedToChangePassword(sAMAccountName)) {
					request.getSession().setAttribute(Constants.SESSION_SAMACCOUNTNAME, sAMAccountName);
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
		else {
			request.getSession().setAttribute(Constants.SESSION_SAMACCOUNTNAMES, sAMAccountNames);
			model.addAttribute("users", sAMAccountNames);

			return "password/pickuser";
		}
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping("/password/reset/{sAMAccountName}")
	public String resetGetWithSAMAccountName(Model model, @PathVariable("sAMAccountName") String sAMAccountName, HttpServletRequest request) throws Exception {
		List<String> sAMAccountNames = new ArrayList<>();

		Object o = request.getSession().getAttribute(Constants.SESSION_SAMACCOUNTNAMES);
		if (o != null && (o instanceof List<?>)) {
			sAMAccountNames = (List<String>) request.getSession().getAttribute(Constants.SESSION_SAMACCOUNTNAMES);
		}
		
		if (!sAMAccountNames.contains(sAMAccountName)) {
			log.error("User picked a sAMAccountName that his/her ssn is not associated with...");

			return "password/error";
		}

		try {
			if (ldapService.isAllowedToChangePassword(sAMAccountName)) {
				request.getSession().setAttribute(Constants.SESSION_SAMACCOUNTNAME, sAMAccountName);
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

	@PostMapping(path = "/password/reset", consumes= "application/x-www-form-urlencoded;charset=UTF-8")
	public String resetPassword(Model model, @Valid @ModelAttribute("newPasswordForm") NewPasswordForm newPasswordForm , BindingResult bindingResult, HttpServletRequest request) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute("newPasswordForm", newPasswordForm);
			return "password/newPassword";
		}

		String sAMAccountName = (String) request.getSession().getAttribute(Constants.SESSION_SAMACCOUNTNAME);
		if (sAMAccountName == null) {
			return "redirect:/password/reset/login";
		}

		try {
			UsernameAndPassword result = ldapService.resetPassword(sAMAccountName, newPasswordForm.getNewPassword());

			boolean desktop = false;
			Object o = request.getSession().getAttribute(Constants.SESSION_DESKTOP);
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
		finally {
			SecurityUtil.logout();
		}

		model.addAttribute("message", "Failed to change password.");

		return "password/error";
	}
}
