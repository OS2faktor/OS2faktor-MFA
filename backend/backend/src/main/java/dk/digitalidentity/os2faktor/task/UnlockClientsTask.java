package dk.digitalidentity.os2faktor.task;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;

@Component
@EnableScheduling
public class UnlockClientsTask {

	@Autowired
	private ClientDao clientDao;

	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	// check every 2nd minute for clients that should be unlocked
	@Scheduled(cron = "0 0/2 * * * ?")
	@Transactional(rollbackFor = Exception.class)
	private void unlockClients() {
		if (!runScheduled) {
			return;
		}

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
}
