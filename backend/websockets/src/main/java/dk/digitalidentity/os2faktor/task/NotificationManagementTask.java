package dk.digitalidentity.os2faktor.task;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Notification;

@Component
@EnableScheduling
public class NotificationManagementTask {

	// TODO: we need a cleanup task that removes all subscriptions

	@Autowired
	private NotificationDao notificationDao;

	// TODO: when moving this code into our main codebase, we should merge the push-notification and
	//       websocket notification bits... but take High-Availability into consideration, so websockets
	//       are pushed everywhere (but cleanup jobs like this only runs on one instance), and push
	//       notifications are also only handled from one location... actually, do we want to resend
	//       push? No, probably not, so ths subscription table needs to know about the type of
	//       notification, so it does not try to send websocket notifications through push and vice versa
	@Scheduled(fixedRate = 30 * 1000)
	public void resetSubscriptions() throws IOException {
		
		// fascinating method - probably something that requires a bit more thinking
		List<Notification> challenges = notificationDao.getByClientNotifiedTrueAndClientRejectedFalseAndClientAuthenticatedFalse();

		challenges = challenges.stream().filter(s -> s.getCreated().before(substractSeconds(30, new Date()))).collect(Collectors.toList());

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
