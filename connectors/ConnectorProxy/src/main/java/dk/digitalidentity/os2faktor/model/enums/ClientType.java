package dk.digitalidentity.os2faktor.model.enums;

public enum ClientType {
	WINDOWS("html.enum.clienttype.windows"),
	IOS("html.enum.clienttype.ios"),
	ANDROID("html.enum.clienttype.android"),
	CHROME("html.enum.clienttype.chrome"),
	YUBIKEY("html.enum.clienttype.yubikey");
	
	private String message;
	
	private ClientType(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
}
