package dk.digitalidentity.os2faktor.controller.validators;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import dk.digitalidentity.os2faktor.controller.model.PinRegistration;

@Component
public class PinValidator implements Validator {

	@Override
	public boolean supports(Class<?> aClass) {
		return (PinRegistration.class.isAssignableFrom(aClass));
	}

	@Override
	public void validate(Object o, Errors errors) {
		PinRegistration newPinForm = (PinRegistration) o;

		if (!StringUtils.hasLength(newPinForm.getPin())) {
			errors.rejectValue("pin", "html.errors.pin.empty");
		}
		else if (!newPinForm.getPin().equals(newPinForm.getConfirm())) {
			errors.rejectValue("confirm", "html.errors.pin.confirm.match");
		}
		else if (!isLegalPin(newPinForm.getPin())) {
			errors.rejectValue("pin", "html.errors.pin.bad");
		}
	}

	private boolean isLegalPin(String pinString) {
		// already checked, but let's avoid out of bound issues
		if (pinString.length() != 4) {
			return false;
		}

		char[] pin = pinString.toCharArray();
		if (pin[0] == pin[1] && pin[1] == pin[2] && pin[2] == pin[3]) {
			return false;
		}

		// increasing order of numbers
		if (pin[0] == (pin[1] - 1) && pin[1] == (pin[2] - 1) && pin[2] == (pin[3] - 1)) {
			return false;
		}

		// decreasing order of numbers
		if (pin[0] == (pin[1] + 1) && pin[1] == (pin[2] + 1) && pin[2] == (pin[3] + 1)) {
			return false;
		}

		return true;
	}
}
