package dk.digitalidentity.os2faktor.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClientService {

	@Autowired
	private ClientDao clientDao;

	public Client getByDeviceId(String deviceId) {
		return clientDao.findByDeviceId(deviceId);
	}

	public Client getByYubikeyUid(String uid) {
		return clientDao.findByYubikeyUid(uid);
	}
	
	public Client save(Client client) {
		return clientDao.save(client);
	}

	public void saveAll(List<Client> clients) {
		clientDao.saveAll(clients);
	}

	@Transactional
	public void deleteAncientClients() {

		// disabled 3 months ago, delete now
		int deleted = clientDao.deleteOldDisabledClients();
		log.info("Deleted " + deleted + " old disabled clients");
		
		// never used, registered 6 months ago, delete it
		deleted = clientDao.deleteOldUnusedClients();
		log.info("Deleted " + deleted + " never registered clients");
		
		// not used in 12 months, delete it
		deleted = clientDao.deleteClientsNotUsedIn12Months();
		log.info("Deleted " + deleted + " old clients not used the last 12 months");
	}

	@Transactional
	public void unlockClients() {
		List<Client> clients = clientDao.findByLockedTrue();
		Date now = new Date();

		for (Client client : clients) {
			if (client.isLocked() && now.after(client.getLockedUntil())) {
				client.setLockedUntil(null);
				client.setLocked(false);
			}
		}

		clientDao.saveAll(clients);
	}

	public List<Client> getByNotificationKey(String notificationKey) {
		return clientDao.findByNotificationKey(notificationKey);
	}
}
