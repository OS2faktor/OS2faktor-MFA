package dk.digitalidentity.os2faktor.controller.model;

public enum FailedFlow {
	NEMID("html.failedflow.nemid"),
	REGISTRATION("html.failedflow.registration"),
	SELF_SERVICE("html.failedflow.selfservice"),
	PIN("html.failedflow.pin");

	private String message;
	
	private FailedFlow(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
}
