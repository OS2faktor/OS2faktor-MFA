package dk.digitalidentity.os2faktor.task;

import dk.digitalidentity.os2faktor.service.NotificationService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class RemoveOldChallengeHistoryTask {
	
	@Autowired
	private NotificationService notificationService;
	
	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	// delete from notifications from history table that are over a month old
	@Scheduled(cron = "0 30 19 * * ?")
	private void removeChallenges() {
		if (!runScheduled) {
			return;
		}

		log.info("Deleting old challenge history");

		notificationService.deleteOldNotificationHistory();

		log.info("Done old challenge history");
	}
}
