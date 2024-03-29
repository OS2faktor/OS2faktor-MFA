package dk.digitalidentity.os2faktor.controller.model;

public enum FailedFlow {
	MIT_ID("html.failedflow.mitid"),
	REGISTRATION("html.failedflow.registration"),
	SELF_SERVICE("html.failedflow.selfservice"),
	PIN("html.failedflow.pin"),
	EXTERNAL_LOGIN("html.failedflow.external_login");

	private String message;
	
	private FailedFlow(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
}
