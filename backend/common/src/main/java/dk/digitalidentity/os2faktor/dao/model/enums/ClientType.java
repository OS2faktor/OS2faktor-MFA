package dk.digitalidentity.os2faktor.dao.model.enums;

public enum ClientType {
	WINDOWS("html.enum.clienttype.windows"),
	IOS("html.enum.clienttype.ios"),
	ANDROID("html.enum.clienttype.android"),
	CHROME("html.enum.clienttype.chrome"),
	EDGE("html.enum.clienttype.edge"),
	YUBIKEY("html.enum.clienttype.yubikey"),
	TOTP("html.enum.clienttype.totp"),
	TOTPH("html.enum.clienttype.totph");
	
	private String message;
	
	private ClientType(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
}
