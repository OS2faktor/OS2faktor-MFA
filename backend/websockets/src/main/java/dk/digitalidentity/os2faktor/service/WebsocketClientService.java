package dk.digitalidentity.os2faktor.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import dk.digitalidentity.os2faktor.api.model.PinResult;
import dk.digitalidentity.os2faktor.api.model.PinResultStatus;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WebsocketClientService {
	private Set<String> seenChallenges = new HashSet<>();

	@Autowired
	private ClientDao clientDao;
	
	@Autowired
	private HashingService hashingService;

	@Autowired
	private NotificationDao notificationDao;

	@Autowired
	private SocketHandler socketHandler;

	@Transactional
	public PinResult handleAcceptReject(String status, String deviceId, String subscriptionKey, String version, String pinCode) {
		// default result - pincode is OKAY
		PinResult result = new PinResult();
		result.setStatus(PinResultStatus.OK);

		// if challenge or client does not match anything in our database, return WRONG_PIN, and then we take it from there
		Notification challenge = notificationDao.findBySubscriptionKey(subscriptionKey);
		Client client = clientDao.findByDeviceId(deviceId);

		if (challenge == null) {
			log.warn("Trying to answer a challenge that does not exist: " + subscriptionKey);

			result.setStatus(PinResultStatus.WRONG_PIN);
		}
		else if (client == null) {
			log.warn("Trying to answer a challenge for a client that does not exist: " + deviceId);
			
			result.setStatus(PinResultStatus.WRONG_PIN);
		}
		else if ("ACCEPT".equals(status)) {
			if (client.isLocked()) {
				log.warn("Client with deviceId: " + client.getDeviceId() + " tried to accept a challenge while being locked out." );

				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

				result.setStatus(PinResultStatus.LOCKED);
				result.setLockedUntil(format.format(client.getLockedUntil()));
			}
			else if (challenge.getClient().getPincode() != null) {
				try {
					boolean matches = hashingService.matches(pinCode, challenge.getClient().getPincode());

					if (!matches) {
						log.warn("Wrong pin for device: " + challenge.getClient().getDeviceId());
	
						if (client.getFailedPinAttempts() >= 5) {
							Calendar c = Calendar.getInstance();
							c.add(Calendar.MINUTE, 5);
							Date lockedUntil = c.getTime();
	
							SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
							result.setStatus(PinResultStatus.LOCKED);
							result.setLockedUntil(format.format(lockedUntil));
	
							client.setFailedPinAttempts(0);
							client.setLockedUntil(lockedUntil);
							client.setLocked(true);
						}
						else {
							result.setStatus(PinResultStatus.WRONG_PIN);
	
							client.setFailedPinAttempts(client.getFailedPinAttempts() + 1);
						}
					}
				}
				catch (Exception ex) {
					log.error("Failed to check matching pincode", ex);
					
					// the best we can do - at least the client will act in a sane way
					result.setStatus(PinResultStatus.WRONG_PIN);
				}
			}

			// noone objected, so challenge has been approved by client
			if (result.getStatus().equals(PinResultStatus.OK)) {
				client.setFailedPinAttempts(0);

				challenge.setClientAuthenticated(true);
				challenge.setClientResponseTimestamp(new Date());
			}
		}
		else if ("REJECT".equals(status)) {
			challenge.setClientRejected(true);
			challenge.setClientResponseTimestamp(new Date());
		}

		if (challenge != null) {
			notificationDao.save(challenge);
	
			client.setUseCount(client.getUseCount() + 1);
			client.setClientVersion(version);
			clientDao.save(client);
		}
		
		return result;
	}

	@Transactional
	public void scanForSubscriptions() {
		StopWatch stopWatch = new StopWatch("scanForSubscriptions");

		stopWatch.start("fetchFromDB");
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, -30); // anything older than 30 seconds is not relevant
		
		Date timestamp = calendar.getTime();

		List<Notification> challenges = notificationDao.findByCreatedAfterAndClientType(timestamp, ClientType.WINDOWS.toString());
		stopWatch.stop();

		stopWatch.start("iterateChallenges");
		for (Notification challenge : challenges) {
			// filter to ensure we only send a given challenge once
			if (!seenChallenges.contains(challenge.getSubscriptionKey())) {
				seenChallenges.add(challenge.getSubscriptionKey());
				
				socketHandler.sendNotification(challenge.getClient(), challenge);
			}
		}
		stopWatch.stop();

		long time = stopWatch.getTotalTimeMillis();
		if (time > 1000) {
			log.warn("It took a while (" + time + " ms) to complete task.... while checking: " + challenges.size() + " challenges. Details = " + stopWatch.prettyPrint());
		}
	}

	public void resetSeenChallenges() {
		seenChallenges.clear();
	}
}
