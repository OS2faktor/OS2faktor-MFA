package dk.digitalidentity.os2faktor.security;

import dk.digitalidentity.os2faktor.dao.model.Server;

public class AuthorizedServerHolder {
	private static final ThreadLocal<Server> serverHolder = new ThreadLocal<>();
	
	public static void setServer(Server server) {
		serverHolder.set(server);
	}
	
	public static Server getServer() {
		return serverHolder.get();
	}
	
	public static void clear() {
		serverHolder.remove();
	}
}
