package dk.digitalidentity.os2faktor.controller.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewPasswordForm {
	private String newPassword;
	private String confirmPassword;
}
