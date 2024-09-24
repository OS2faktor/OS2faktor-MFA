package dk.digitalidentity.os2faktor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Subscription.Keys;

@Slf4j
@Service
public class PushServiceWrapper {

	@Autowired
	private PushService pushService;
	
	@Async
	public void push(String endpoint, String key, String auth, String json, String deviceId) {
		try {
			pushService.send(new Notification(new Subscription(endpoint, new Keys(key, auth)), json));
		}
		catch (Exception ex) {
			log.error("Failed to send push notification to edge/chrome client: " + deviceId, ex);
		}		
	}
}
