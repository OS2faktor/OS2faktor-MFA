package dk.digitalidentity.os2faktor.api;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.api.model.Challenge;
import dk.digitalidentity.os2faktor.api.model.PinResult;
import dk.digitalidentity.os2faktor.api.model.PinResultStatus;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
public class ClientApi {
	
	@Autowired
	private NotificationDao notificationDao;
	
	@Autowired
	private ClientDao clientDao;

	@GetMapping("/api/client")
	public ResponseEntity<Challenge> poll(@RequestHeader("deviceId") String deviceId) {
		Client client = clientDao.getByDeviceId(deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().build();
		}

		List<Notification> challenges = notificationDao.getByClientAndClientRejectedFalseAndClientAuthenticatedFalseAndClientLockedFalse(client);

		if (challenges.size() == 0) {
			return ResponseEntity.notFound().build();
		}

		Challenge challenge = new Challenge();
		challenge.setChallenge(challenges.get(0).getChallenge());
		challenge.setUuid(challenges.get(0).getSubscriptionKey());
		challenge.setServerName(challenges.get(0).getServerName());

		return new ResponseEntity<>(challenge, HttpStatus.OK);
	}

	@PutMapping("/api/client/{uuid}/accept")
	public ResponseEntity<?> accept(@PathVariable("uuid") String uuid, @RequestHeader("deviceId") String deviceId, @RequestHeader("clientVersion") String clientVersion, @RequestHeader(name = "pinCode", required = false) String pinCode) {
		Notification challenge = notificationDao.getBySubscriptionKey(uuid);
		if (challenge != null) {
			if (!challenge.getClient().getDeviceId().equals(deviceId)) {
				log.error("Failed to accept challenge. Client with deviceId " + deviceId + " does not own challenge with subscriptionKey " + challenge.getSubscriptionKey());
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			Client client = challenge.getClient();
			
			if (client.isLocked()) {
				log.warn("Client with deviceId: " + client.getDeviceId() + " tried to accept a challenge while being locked out." );

				PinResult result = new PinResult();
				result.setStatus(PinResultStatus.LOCKED);
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
				result.setLockedUntil(format.format(client.getLockedUntil()));

				return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
			}

			if (challenge.getClient().getPincode() != null && !challenge.getClient().getPincode().equals(pinCode)) {
				log.warn("Wrong pin for device: " + challenge.getClient().getDeviceId());

				PinResult result = new PinResult();

				if (client.getFailedPinAttempts() >= 5) {
					Calendar c = Calendar.getInstance();
					c.add(Calendar.HOUR_OF_DAY, 1);
					Date lockedUntil = c.getTime();

					result.setStatus(PinResultStatus.LOCKED);
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					result.setLockedUntil(format.format(lockedUntil));

					client.setFailedPinAttempts(0);
					client.setLockedUntil(lockedUntil);
					client.setLocked(true);
				}
				else {
					client.setFailedPinAttempts(client.getFailedPinAttempts() + 1);
					result.setStatus(PinResultStatus.WRONG_PIN);
				}

				clientDao.save(client);

				return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
			}
			
			// if the client already answered, do not do anything
			if (challenge.isClientRejected() || challenge.isClientAuthenticated()) {
				return new ResponseEntity<>(HttpStatus.OK);
			}
			
			challenge.setClientAuthenticated(true);
			notificationDao.save(challenge);

			// update useCount (side-effect: also updates lastUsed timestamp)
			client.setUseCount(client.getUseCount() + 1);
			client.setFailedPinAttempts(0);
			if (!StringUtils.isEmpty(clientVersion)) {
				client.setClientVersion(clientVersion);
			}

			clientDao.save(client);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PutMapping("/api/client/{uuid}/reject")
	public ResponseEntity<Challenge> reject(@PathVariable("uuid") String uuid, @RequestHeader("deviceId") String deviceId, @RequestHeader("clientVersion") String clientVersion) {
		Notification challenge = notificationDao.getBySubscriptionKey(uuid);
		if (challenge != null) {
			if (!challenge.getClient().getDeviceId().equals(deviceId)) {
				log.error("Failed to reject challenge. Client with deviceId " + deviceId + " does not own challlenge with subscriptionKey " + challenge.getSubscriptionKey());
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			// if the client already answered, do not do anything
			if (challenge.isClientRejected() || challenge.isClientAuthenticated()) {
				return new ResponseEntity<>(HttpStatus.OK);
			}

			challenge.setClientRejected(true);
			notificationDao.save(challenge);
			
			// update useCount (side-effect: also updates lastUsed timestamp)
			Client client = challenge.getClient();
			client.setUseCount(client.getUseCount() + 1);
			if (!StringUtils.isEmpty(clientVersion)) {
				client.setClientVersion(clientVersion);
			}
			clientDao.save(client);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(path="/api/client", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteClient(@RequestHeader("deviceId") String deviceId) {
		Client client = clientDao.getByDeviceId(deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().build();
		}

		client.setDisabled(true);
		clientDao.save(client);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
