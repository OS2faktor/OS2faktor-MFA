package dk.digitalidentity.os2faktor.controller.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Registration {

	@NotNull(message = "Navnet skal være udfyldt")
	@Size(min = 3, max = 128, message = "Navn skal være mindst 3 og maksimalt 128 tegn")
	@Pattern(regexp = "^[^<>\"]*$", message = "Navnet må ikke indeholde tegnene <, >, eller \"")
	private String name;
}
