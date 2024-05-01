package dk.digitalidentity.os2faktor.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.service.ExternalLoginSessionService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class RemoveExternalLoginSessionsTask {

	@Autowired
	private ExternalLoginSessionService externalLoginSessionService;

	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	@Scheduled(cron = "0 10 2 * * ?")
	private void removeChallenges() {
		if (!runScheduled) {
			return;
		}

		log.info("Deleting all external logins");
		
		externalLoginSessionService.deleteAll();
		
		log.info("Done deleting all external logins");
	}
}
