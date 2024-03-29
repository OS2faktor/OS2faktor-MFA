package dk.digitalidentity.os2faktor.task;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.service.SocketHandler;

@Component
@EnableScheduling
public class ChallengeTask {

	@Autowired
	private NotificationDao notificationDao;

	@Autowired
	private SocketHandler socketHandler;

	@Scheduled(fixedRate = 2 * 1000)
	private void scanForSubscriptions() {
		List<Notification> challenges = notificationDao.getByClientNotifiedFalseAndClientType(ClientType.WINDOWS);

		for (Notification challenge : challenges) {
			socketHandler.sendNotification(challenge.getClient(), challenge);
		}
	}
}
