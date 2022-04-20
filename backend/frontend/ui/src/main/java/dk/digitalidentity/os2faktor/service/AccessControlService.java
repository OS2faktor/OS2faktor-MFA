package dk.digitalidentity.os2faktor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.service.model.ClientOrUser;

@Service
public class AccessControlService {

	@Autowired
	private UserService userService;
	
	public boolean doesAuthenticatedEntityHaveAccessToDeviceId(ClientOrUser clientOrUser, String deviceId) {

		// a client always have access to itself
		if (clientOrUser.hasClient() && clientOrUser.getClient().getDeviceId().equals(deviceId)) {
			return true;
		}
		
		// otherwise we need a user
		if (clientOrUser.getUser() == null) {
			return false;
		}
		
		// reload user, to ensure we get the updated data
		User user = userService.getByEncryptedAndEncodedSsn(clientOrUser.getUser().getSsn());

		if (user.getClients() != null) {
			for (Client c : user.getClients()) {
				if (c.getDeviceId().equals(deviceId)) {
					return true;
				}
			}
		}

		return false;
	}
}
