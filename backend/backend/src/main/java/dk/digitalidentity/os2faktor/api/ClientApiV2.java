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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.api.dto.ClientStatus;
import dk.digitalidentity.os2faktor.api.dto.RegisterRequest;
import dk.digitalidentity.os2faktor.api.dto.RegisterResponse;
import dk.digitalidentity.os2faktor.api.dto.SetPinRequest;
import dk.digitalidentity.os2faktor.api.dto.ValidatePinRequest;
import dk.digitalidentity.os2faktor.api.model.PinResult;
import dk.digitalidentity.os2faktor.api.model.PinResultStatus;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.service.ClientService;
import dk.digitalidentity.os2faktor.service.HashingService;
import dk.digitalidentity.os2faktor.service.IdGenerator;
import dk.digitalidentity.os2faktor.service.PushNotificationSenderService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
public class ClientApiV2 {
	
	@Autowired
	private HashingService hashingService;
	
	@Autowired
	private ClientService clientService;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private PushNotificationSenderService snsService;

	@GetMapping("/api/client/v2/status")
	public ResponseEntity<ClientStatus> getStatus(@RequestHeader("deviceId") String deviceId) {
		ClientStatus response = new ClientStatus();

		Client client = clientService.getByDeviceId(deviceId);
		if (client != null) {
			response.setDisabled(client.isDisabled());
			response.setPinProtected(!StringUtils.isEmpty(client.getPincode()));
			response.setNemIdRegistered(client.getUser() != null);
			
			if (!client.isDisabled()) {
				client.setLastUsed(new Date());
				clientService.save(client);
			}
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// xapi for public/unauthenticated
	@PostMapping("/xapi/client/v2/register")
	public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request, @RequestHeader("clientVersion") String clientVersion) {
		RegisterResponse response = new RegisterResponse();

		// verify that pin is valid
		if (!isValidPin(request.getPincode())) {
			response.setInvalidPin(true);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		
		Client client = new Client();
		client.setUseCount(0);
		client.setDeviceId(idGenerator.generateDeviceId());
		client.setName(request.getName());
		client.setToken(request.getToken());
		client.setType(request.getType());
		client.setClientVersion(clientVersion);

		// Generate apiKey
		String apiKey = idGenerator.generateUuid();

		// Encode it and save it on client
		try {
			client.setPincode(hashingService.encryptAndEncodeString(request.getPincode()));
			client.setApiKey(hashingService.encryptAndEncodeString(apiKey));
		}
		catch (Exception ex) {
			log.error("Failed to encrypt and encode apiKey", ex);

			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (!StringUtils.isEmpty(client.getToken())) {
			if (client.getType().equals(ClientType.EDGE)) {
				;
			}
			else {
				try {
					String notificationKey = snsService.createEndpoint(client.getToken(), client.getDeviceId(), client.getType());
					if (!StringUtils.isEmpty(notificationKey)) {
						client.setNotificationKey(notificationKey);
						
						// see if there are any existing clients with this notification key, and disable them
						// as they are old clients installed on the same device (and now overwritten by the new client)
						List<Client> existingClients = clientService.getByNotificationKey(notificationKey);
						for (Client existingClient : existingClients) {
							if (!existingClient.isDisabled()) {
								existingClient.setDisabled(true);
								clientService.save(existingClient);
							}
						}
					}
				}
				catch (Exception ex) {
					log.error("Failed to register token at AWS SNS", ex);
					
					return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
		}

		clientService.save(client);
		
		response.setSuccess(true);
		response.setApiKey(apiKey);
		response.setDeviceId(client.getDeviceId());

		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping("/api/client/v2/setPin")
	public ResponseEntity<PinResult> setPin(@RequestHeader("deviceId") String deviceId, @RequestHeader("clientVersion") String clientVersion, @RequestBody SetPinRequest request) {
		PinResult response = new PinResult();
		response.setStatus(PinResultStatus.WRONG_PIN);

		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		
		if (client.isLocked()) {
			log.warn("Client with deviceId: " + client.getDeviceId() + " tried to set a pincode while being locked" );

			response.setStatus(PinResultStatus.LOCKED);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			response.setLockedUntil(format.format(client.getLockedUntil()));

			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		
		// verify that pin is valid
		if (!isValidPin(request.getNewPin())) {
			response.setStatus(PinResultStatus.INVALID_NEW_PIN);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		// if an existing pincode is present, the supplied old pincode MUST match
		if (client.getPincode() != null && client.getPincode().length() > 0) {
			boolean matches = false;

			if (request.getOldPin() != null && request.getOldPin().length() > 0) {
				try {
					matches = hashingService.matches(request.getOldPin(), client.getPincode());
				}
				catch (Exception ex) {
					log.error("Failed to check matching pincode for client " + client.getDeviceId(), ex);
	
					return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}

			if (!matches) {
				response.setStatus(PinResultStatus.WRONG_PIN);
				log.warn("Wrong pin for device: " + client.getDeviceId());

				client.setFailedPinAttempts(client.getFailedPinAttempts() + 1);
				if (client.getFailedPinAttempts() >= 5) {
					Calendar c = Calendar.getInstance();
					c.add(Calendar.HOUR_OF_DAY, 1);
					Date lockedUntil = c.getTime();

					client.setLockedUntil(lockedUntil);
					client.setLocked(true);
					
					response.setStatus(PinResultStatus.LOCKED);
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					response.setLockedUntil(format.format(client.getLockedUntil()));
				}
				
				return new ResponseEntity<>(response, HttpStatus.OK);
			}
			else {
				client.setFailedPinAttempts(0);
			}
		}

		// update clientVersion and pin
		if (!StringUtils.isEmpty(clientVersion)) {
			client.setClientVersion(clientVersion);

			try {
				client.setPincode(hashingService.encryptAndEncodeString(request.getNewPin()));
			}
			catch (Exception ex) {
				log.error("Failed to encrypt and encode pin", ex);

				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		clientService.save(client);

		response.setStatus(PinResultStatus.OK);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/api/client/v2/validatePin")
	public ResponseEntity<PinResult> validatePin(@RequestHeader("deviceId") String deviceId, @RequestHeader("clientVersion") String clientVersion, @RequestBody ValidatePinRequest request) {
		PinResult response = new PinResult();
		response.setStatus(PinResultStatus.WRONG_PIN);

		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		
		if (client.isLocked()) {
			log.warn("Client with deviceId: " + client.getDeviceId() + " tried to validate a pincode while being locked" );

			response.setStatus(PinResultStatus.LOCKED);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			response.setLockedUntil(format.format(client.getLockedUntil()));

			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		
		if (client.getPincode() == null) {
			log.error("Attempting to validate pin on a client that does not have a pin: " + deviceId);

			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		// update clientVersion
		if (!StringUtils.isEmpty(clientVersion)) {
			client.setClientVersion(clientVersion);
		}

		boolean matches = false;
		try {
			matches = hashingService.matches(request.getPincode(), client.getPincode());
		}
		catch (Exception ex) {
			log.error("Failed to check matching pincode for client " + client.getDeviceId(), ex);

			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (!matches) {
			log.warn("Wrong pin for device: " + client.getDeviceId());

			client.setFailedPinAttempts(client.getFailedPinAttempts() + 1);
			if (client.getFailedPinAttempts() >= 5) {
				Calendar c = Calendar.getInstance();
				c.add(Calendar.HOUR_OF_DAY, 1);
				Date lockedUntil = c.getTime();

				client.setLockedUntil(lockedUntil);
				client.setLocked(true);
				
				response.setStatus(PinResultStatus.LOCKED);
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				response.setLockedUntil(format.format(client.getLockedUntil()));
			}
		}
		else {
			client.setFailedPinAttempts(0);

			response.setStatus(PinResultStatus.OK);
		}

		clientService.save(client);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	public boolean isValidPin(String pinString) {
		// already checked, but let's avoid out of bound issues
		if (pinString.length() != 4) {
			return false;
		}

		char[] pin = pinString.toCharArray();
		if (pin[0] == pin[1] && pin[1] == pin[2] && pin[2] == pin[3]) {
			return false;
		}

		// increasing order of numbers
		if (pin[0] == (pin[1] - 1) && pin[1] == (pin[2] - 1) && pin[2] == (pin[3] - 1)) {
			return false;
		}

		// decreasing order of numbers
		if (pin[0] == (pin[1] + 1) && pin[1] == (pin[2] + 1) && pin[2] == (pin[3] + 1)) {
			return false;
		}

		return true;
	}
}
