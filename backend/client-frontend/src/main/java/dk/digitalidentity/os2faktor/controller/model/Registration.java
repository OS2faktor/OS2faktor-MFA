package dk.digitalidentity.os2faktor.controller.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Registration {

	// TODO: translate validation messages
	@NotNull
	@Size(min = 3, max = 128)
	private String name;
}
