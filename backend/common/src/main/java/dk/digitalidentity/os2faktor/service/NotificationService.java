package dk.digitalidentity.os2faktor.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import dk.digitalidentity.os2faktor.dao.NotificationHistoryDao;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.NotificationHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2faktor.dao.NotificationDao;

@Service
public class NotificationService {

	@Autowired
	private NotificationDao notificationDao;

	@Autowired
	private NotificationHistoryDao notificationHistoryDao;

	@Transactional
	public void deleteOldNotifications() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -5);
		
		Date timestamp = calendar.getTime();

		List<Notification> expiredNotifications = notificationDao.findByCreatedBefore(timestamp);
		List<NotificationHistory> toBeInserted = convertToHistoryObjects(expiredNotifications);
		notificationHistoryDao.saveAll(toBeInserted);
		notificationDao.deleteAll(expiredNotifications);
	}

	@Transactional
	public void deleteOldNotificationHistory() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -1);

		Date timestamp = calendar.getTime();

		notificationHistoryDao.deleteByCreatedBefore(timestamp);
	}

	private List<NotificationHistory> convertToHistoryObjects(List<Notification> toBeDeleted) {
		if (toBeDeleted == null) {
			return new ArrayList<>();
		}

		return toBeDeleted.stream()
				.map(notification -> NotificationHistory.builder()
					.subscriptionKey(notification.getSubscriptionKey())
					.pollingKey(notification.getPollingKey())
					.created(notification.getCreated())
					.sentTimestamp(notification.getSentTimestamp())
					.clientNotified(notification.isClientNotified())
					.clientAuthenticated(notification.isClientAuthenticated())
					.clientRejected(notification.isClientRejected())
					.clientFetchedTimestamp(notification.getClientFetchedTimestamp())
					.clientResponseTimestamp(notification.getClientResponseTimestamp())
					.challenge(notification.getChallenge())
					.redirectUrl(notification.getRedirectUrl())
					.serverName(notification.getServerName())
					.serverId(notification.getServerId())
					.clientDeviceId(notification.getClient().getDeviceId())
					.passwordless(notification.isPasswordless())
					.clientType(notification.getClientType())
					.build()
				)
				.collect(Collectors.toList());
	}
}
