package dk.digitalidentity.os2faktor.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidatePinRequest {
	private String pincode;
}
