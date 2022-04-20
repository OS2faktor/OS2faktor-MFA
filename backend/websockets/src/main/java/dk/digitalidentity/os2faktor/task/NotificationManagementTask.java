package dk.digitalidentity.os2faktor.task;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;

@Component
@EnableScheduling
public class NotificationManagementTask {

	@Autowired
	private NotificationDao notificationDao;
	
	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	@Scheduled(fixedRate = 30 * 1000)
	public void resetSubscriptions() throws IOException {
		if (!runScheduled) {
			return;
		}

		List<Notification> challenges = notificationDao.getByClientNotifiedTrueAndClientRejectedFalseAndClientAuthenticatedFalse();

		challenges = challenges.stream()
				.filter(s -> s.getClient().getType().equals(ClientType.WINDOWS) && s.getCreated().before(substractSeconds(30, new Date())))
				.collect(Collectors.toList());

		for (Notification subscriptionInfo : challenges) {
			subscriptionInfo.setClientNotified(false);
		}

		notificationDao.saveAll(challenges);
	}

	public Date substractSeconds(Integer variation, Date currentDate) {
		Calendar c = Calendar.getInstance();
		c.setTime(currentDate);
		c.add(Calendar.SECOND, -(variation));

		return c.getTime();
	}
}
