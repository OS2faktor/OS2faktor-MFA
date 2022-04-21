package dk.digitalidentity.os2faktor.controller.model;

import javax.validation.constraints.Size;
import javax.validation.constraints.NotNull;

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
