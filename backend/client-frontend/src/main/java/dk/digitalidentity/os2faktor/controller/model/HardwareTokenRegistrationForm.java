package dk.digitalidentity.os2faktor.controller.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HardwareTokenRegistrationForm {
	private String serial;
	private String code;
}
