package dk.digitalidentity.os2faktor.controller.model;

public enum ErrorType {
	UNKNOWN("html.errortype.unknown"),
	ALREADY_REGISTERED("html.errortype.alreadyregistered"),
	BAD_CREDENTIALS("html.errortype.badcredentials"),
	PIN_ALREADY_ASSIGNED("html.errortype.pinalreadyassigned"),
	UNAUTHORIZED("html.errortype.unauthorized"),
	EXCEPTION("html.errortype.exception"),
	MITID_VALIDATION("html.errortype.mitidvalidation"),
	BAD_REQUEST("html.errortype.badrequest"),
	UNKNOWN_CLIENT("html.errortype.unknownclient"),	
	EXTERNAL_SESSION_EXPIRED("html.errortype.external_session_expired");
	
	private String message;
	
	private ErrorType(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
}
