package dk.digitalidentity.os2faktor.security;

import dk.digitalidentity.os2faktor.dao.model.LoginServiceProvider;

public class AuthorizedLoginServiceProviderHolder {
	private static final ThreadLocal<LoginServiceProvider> loginHolder = new ThreadLocal<>();
	
	public static void setLoginServiceProvider(LoginServiceProvider loginServiceProvider) {
		loginHolder.set(loginServiceProvider);
	}
	
	public static LoginServiceProvider getLoginServiceProvider() {
		return loginHolder.get();
	}
	
	public static void clear() {
		loginHolder.remove();
	}
}
