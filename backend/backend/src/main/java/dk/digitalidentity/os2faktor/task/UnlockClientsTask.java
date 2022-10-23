package dk.digitalidentity.os2faktor.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.service.ClientService;

@Component
@EnableScheduling
public class UnlockClientsTask {

	@Autowired
	private ClientService clientService;

	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	// check every 2nd minute for clients that should be unlocked
	@Scheduled(cron = "30 0/2 * * * ?")
	private void unlockClients() {
		if (!runScheduled) {
			return;
		}

		clientService.unlockClients();
	}
}
