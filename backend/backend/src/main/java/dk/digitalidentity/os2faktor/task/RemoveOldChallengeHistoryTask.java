package dk.digitalidentity.os2faktor.task;

import dk.digitalidentity.os2faktor.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class RemoveOldChallengeHistoryTask {
	
	@Autowired
	private NotificationService notificationService;
	
	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	// Delete from notifications from history table that are over a month old (Runs every Wednesday)
	@Scheduled(cron = "0 #{new java.util.Random().nextInt(55)} 14 * * WED")
	private void removeChallenges() {
		if (!runScheduled) {
			return;
		}

		notificationService.deleteOldNotificationHistory();
	}
}
