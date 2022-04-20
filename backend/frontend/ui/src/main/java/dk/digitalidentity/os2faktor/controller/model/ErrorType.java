package dk.digitalidentity.os2faktor.controller.model;

public enum ErrorType {
	UNKNOWN("html.errortype.unknown"),
	NEMID_VALIDATION("html.errortype.nemidvalidation"),
	NEMID_VALIDATION_KNOWN_ERROR("html.errortype.nemidvalidation"),
	NOT_POCES("html.errortype.notpoces"),
	NO_CPR("html.errortype.nocpr"),
	EXCEPTION("html.errortype.exception"),
	BAD_CERTIFICATE("html.errortype.badcertificate"),
	ALREADY_REGISTERED("html.errortype.alreadyregistered"),
	UNKNOWN_CLIENT("html.errortype.unknownclient"),
	BAD_REQUEST("html.errortype.badrequest"),
	BAD_CREDENTIALS("html.errortype.badcredentials"),
	PIN_ALREADY_ASSIGNED("html.errortype.pinalreadyassigned"),
	UNAUTHORIZED("html.errortype.unauthorized"),
	EXTERNAL_SESSION_EXPIRED("html.errortype.external_session_expired");
	
	private String message;
	
	private ErrorType(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
}
