package dk.digitalidentity.os2faktor.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.service.WebsocketClientService;

@Component
@EnableScheduling
public class ChallengeTask {

	@Autowired
	private WebsocketClientService websocketClientService;
	
	@Scheduled(fixedRate = 1500)
	private void scanForSubscriptions() {
		websocketClientService.scanForSubscriptions();
	}
	
	// reset at 01:00
	@Scheduled(cron = "0 0 1 * * ?")
	private void resetSeenChallenges() {
		websocketClientService.resetSeenChallenges();
	}
}
