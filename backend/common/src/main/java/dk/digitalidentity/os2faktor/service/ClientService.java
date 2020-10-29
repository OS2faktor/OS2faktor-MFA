package dk.digitalidentity.os2faktor.service;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2faktor.dao.ClientDao;

@Service
public class ClientService {

	@Autowired
	private ClientDao clientDao;

	@Transactional
	public void deleteAncientClients() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -3);
		
		Date timestamp = calendar.getTime();

		clientDao.deleteByDisabledTrueAndLastUsedIsNullOrLastUsedBefore(timestamp);
	}
}
