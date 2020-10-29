package dk.digitalidentity.os2faktor.service;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2faktor.dao.NotificationDao;

@Service
public class NotificationService {

	@Autowired
	private NotificationDao notificationDao;

	@Transactional
	public void deleteOldNotifications() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -5);
		
		Date timestamp = calendar.getTime();

		notificationDao.deleteByCreatedBefore(timestamp);
	}
}
