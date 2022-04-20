package dk.digitalidentity.os2faktor.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.service.ClientService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class RemoveOldClientsTask {
	
	@Autowired
	private ClientService clientService;
	
	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	@Scheduled(cron = "${cron.removeOldClients:0 0 21 * * SAT}")
	private void removeClients() {
		if (!runScheduled) {
			return;
		}
		
		log.info("Running deleteAncientClients");

		clientService.deleteAncientClients();
	}
}
