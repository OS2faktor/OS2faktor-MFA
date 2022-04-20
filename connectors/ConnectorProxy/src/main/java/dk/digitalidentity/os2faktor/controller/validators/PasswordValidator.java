package dk.digitalidentity.os2faktor.controller.validators;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import dk.digitalidentity.os2faktor.controller.model.NewPasswordForm;

@Component
public class PasswordValidator implements Validator {

	@Override
	public boolean supports(Class<?> aClass) {
		return (NewPasswordForm.class.isAssignableFrom(aClass));
	}

	@Override
	public void validate(Object o, Errors errors) {
		NewPasswordForm newPasswordForm = (NewPasswordForm) o;

		if (StringUtils.isEmpty(newPasswordForm.getNewPassword())) {
			errors.rejectValue("newPassword", "html.errors.password.empty");
		}
		else if (!newPasswordForm.getNewPassword().equals(newPasswordForm.getConfirmPassword())) {
			errors.rejectValue("confirmPassword", "html.errors.confirmPassword.match");
		}
		else if (!goodPassword(newPasswordForm.getNewPassword())) {
			errors.rejectValue("newPassword", "html.errors.password.bad");
		}
	}

	private boolean goodPassword(String newPassword) {
		boolean hasLetter = false;
		boolean hasDigit = false;
		
		if (newPassword.length() < 8) {
			return false;
		}
		
		for (char c : newPassword.toCharArray()) {
			if (Character.isDigit(c)) {
				hasDigit = true;
			}
			
			if (Character.isLetter(c)) {
				hasLetter = true;
			}
		}
		
		if (!hasDigit || !hasLetter) {
			return false;
		}

		return true;	
	}
}
