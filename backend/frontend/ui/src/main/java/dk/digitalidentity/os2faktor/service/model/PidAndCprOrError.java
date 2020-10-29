package dk.digitalidentity.os2faktor.service.model;

import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PidAndCprOrError {
	private String pid;
	private String cpr;
	private ErrorType error;
	private String errorCode;
	
	public PidAndCprOrError(String pid, String cpr) {
		this.pid = pid;
		this.cpr = cpr;
	}

	public PidAndCprOrError(ErrorType error, String errorCode) {
		this.error = error;
		this.errorCode = errorCode;
	}

	public boolean hasError() {
		return error != null;
	}
}
