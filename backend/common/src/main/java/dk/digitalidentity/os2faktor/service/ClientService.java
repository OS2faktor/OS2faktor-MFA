package dk.digitalidentity.os2faktor.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;

@Service
public class ClientService {

	@Autowired
	private ClientDao clientDao;

	public Client getByDeviceId(String deviceId) {
		return clientDao.getByDeviceId(deviceId);
	}

	public Client getByYubikeyUid(String uid) {
		return clientDao.getByYubikeyUid(uid);
	}
	
	public Client save(Client client) {
		return clientDao.save(client);
	}

	public void saveAll(List<Client> clients) {
		clientDao.saveAll(clients);
	}

	@Transactional
	public void deleteAncientClients() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -6);

		Date timestamp = calendar.getTime();

		// disabled 6 months ago, delete now
		clientDao.deleteByDisabledTrueAndLastUsedIsNullOrLastUsedBefore(timestamp);
		
		// last used 9 months ago, no activity, delete now
		calendar.add(Calendar.MONTH, -3);
		clientDao.deleteByLastUsedIsNullAndCreatedBefore(calendar.getTime());
		clientDao.deleteByLastUsedIsNotNullAndLastUsedBefore(calendar.getTime());
	}

	
	@Transactional
	public void unlockClients() {
		List<Client> clients = clientDao.getByLockedTrue();
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
		return clientDao.getByNotificationKey(notificationKey);
	}
}
