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
	
	public Client save(Client client) {
		return clientDao.save(client);
	}

	public void saveAll(List<Client> clients) {
		clientDao.saveAll(clients);
	}

	@Transactional
	public void deleteAncientClients() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -3);

		Date timestamp = calendar.getTime();

		clientDao.deleteByDisabledTrueAndLastUsedIsNullOrLastUsedBefore(timestamp);
		
		// six months ago
		calendar.add(Calendar.MONTH, -3);
		clientDao.deleteByLastUsedIsNullAndCreatedBefore(calendar.getTime());
		clientDao.deleteByLastUsedIsNotNullAndLastUsedBefore(calendar.getTime());
	}

	public List<Client> getByNotificationKey(String notificationKey) {
		return clientDao.getByNotificationKey(notificationKey);
	}
}
