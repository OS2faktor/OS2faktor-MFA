package dk.digitalidentity.os2faktor.controller.model;

public enum ErrorType {
	EXCEPTION("html.errortype.exception"),
	UNKNOWN_CLIENT("html.errortype.unknownclient"),
	BAD_CREDENTIALS("html.errortype.badcredentials");
	
	private String message;
	
	private ErrorType(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
}
