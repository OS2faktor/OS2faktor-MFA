package dk.digitalidentity.os2faktor.controller.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SsnAssignmentForm {
	private String ssn;
	private String os2faktorId;
}
