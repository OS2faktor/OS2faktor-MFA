package dk.digitalidentity.os2faktor.service.model;

import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientOrUser {
	private User user;
	private Client client;
	
	public ClientOrUser(User user) {
		this.user = user;
	}
	
	public ClientOrUser(Client client) {
		this.client = client;
	}
	
	public boolean hasClient() {
		return client != null;
	}
	
	public boolean hasUser() {
		return user != null;
	}
}
