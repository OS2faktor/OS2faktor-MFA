package dk.digitalidentity.os2faktor.api;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.api.model.Challenge;
import dk.digitalidentity.os2faktor.api.model.PinResult;
import dk.digitalidentity.os2faktor.api.model.PinResultStatus;
import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.service.ClientService;
import dk.digitalidentity.os2faktor.service.HashingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
public class ClientApi {
	
	@Autowired
	private NotificationDao notificationDao;

	@Autowired
	private HashingService hashingService;
	
	@Autowired
	private ClientService clientService;
	
	@GetMapping("/api/client")
	public ResponseEntity<Challenge> poll(@RequestHeader("deviceId") String deviceId) {
		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().build();
		}

		List<Notification> challenges = notificationDao.findByClientAndClientRejectedFalseAndClientAuthenticatedFalseAndClientLockedFalse(client);

		if (challenges.size() == 0) {
			return ResponseEntity.notFound().build();
		}
		
		// sort by date descending to get the latest in case there are more than one
		Notification notification = challenges.stream().sorted((c1, c2) -> c2.getCreated().compareTo(c1.getCreated())).findFirst().orElse(null);

		Challenge challenge = new Challenge();
		challenge.setUuid(notification.getSubscriptionKey());
		challenge.setServerName(notification.getServerName());
		if (notification.isPasswordless()) {
			// send 2 chars, as that will trigger a different UI in client
			challenge.setChallenge("??");
		}
		else {
			challenge.setChallenge(notification.getChallenge());
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		challenge.setTts("kl " + sdf.format(notification.getCreated()));

		// and fetched it is then
		notification.setClientFetchedTimestamp(new Date());
		notificationDao.save(notification);

		return new ResponseEntity<>(challenge, HttpStatus.OK);
	}

	@PutMapping("/api/client/{uuid}/accept")
	public ResponseEntity<?> accept(@PathVariable("uuid") String uuid, @RequestHeader("deviceId") String deviceId, @RequestHeader("clientVersion") String clientVersion, @RequestHeader(name = "pinCode", required = false) String pinCode, @RequestHeader(name = "roaming", required = false) boolean roaming, @RequestHeader(name = "passwordlessChallenge", required = false, defaultValue = "") String passwordlessChallenge) {
		Notification challenge = notificationDao.findBySubscriptionKey(uuid);
		if (challenge != null) {
			if (!challenge.getClient().getDeviceId().equals(deviceId)) {
				log.warn("Failed to accept challenge. Client with deviceId " + deviceId + " does not own challenge with subscriptionKey " + challenge.getSubscriptionKey());
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			Client client = challenge.getClient();
			
			if (StringUtils.hasLength(clientVersion)) {
				client.setClientVersion(clientVersion);
			}
			
			if (client.isLocked()) {
				log.warn("Client with deviceId: " + client.getDeviceId() + " tried to accept a challenge while being locked out." );

				PinResult result = new PinResult();
				result.setStatus(PinResultStatus.LOCKED);
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				result.setLockedUntil(format.format(client.getLockedUntil()));

				return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
			}

			if (client.getPincode() != null) {
				boolean matches = false;
				try {
					matches = hashingService.matches(pinCode, client.getPincode());
				}
				catch (Exception ex) {
					log.error("Failed to check matching pincode for client " + client.getDeviceId(), ex);
					return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
				}

				if (!matches) {
					log.warn("Wrong pin for device: " + challenge.getClient().getDeviceId());

					PinResult result = wrongPinResult(client);
					clientService.save(client);

					return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
				}
			}

			// is this a passwordless flow, then validate the code
			if (client.isPasswordless() && challenge.isPasswordless()) {
				if (!Objects.equals(challenge.getChallenge(), passwordlessChallenge)) {
					// we are "abusing" the current pin-system for this, as we want to trigger the same locking flow for wrong pins
					PinResult result = wrongPinResult(client);
					clientService.save(client);

					return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
				}
			}
			
			// if the client already answered, do not do anything
			if (challenge.isClientRejected() || challenge.isClientAuthenticated()) {
				return new ResponseEntity<>(HttpStatus.OK);
			}
			
			challenge.setClientAuthenticated(true);
			challenge.setClientResponseTimestamp(new Date());
			notificationDao.save(challenge);

			// update useCount (side-effect: also updates lastUsed timestamp)
			client.setUseCount(client.getUseCount() + 1);
			client.setFailedPinAttempts(0);
			
			// update roaming
			client.setRoaming(roaming);

			clientService.save(client);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PutMapping("/api/client/{uuid}/reject")
	public ResponseEntity<Challenge> reject(@PathVariable("uuid") String uuid, @RequestHeader("deviceId") String deviceId, @RequestHeader("clientVersion") String clientVersion) {
		Notification challenge = notificationDao.findBySubscriptionKey(uuid);
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
			challenge.setClientResponseTimestamp(new Date());
			notificationDao.save(challenge);
			
			// update useCount (side-effect: also updates lastUsed timestamp)
			Client client = challenge.getClient();
			client.setUseCount(client.getUseCount() + 1);
			if (StringUtils.hasLength(clientVersion)) {
				client.setClientVersion(clientVersion);
			}
			
			clientService.save(client);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/api/client")
	public ResponseEntity<?> deleteClient(@RequestHeader("deviceId") String deviceId) {
		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().build();
		}

		client.setDisabled(true);
		clientService.save(client);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	private PinResult wrongPinResult(Client client) {
		PinResult result = new PinResult();

		if (client.getFailedPinAttempts() >= 5) {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MINUTE, 5);
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

		return result;
	}
}
