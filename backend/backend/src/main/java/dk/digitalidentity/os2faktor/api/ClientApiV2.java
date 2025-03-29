package dk.digitalidentity.os2faktor.api;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import dk.digitalidentity.os2faktor.api.dto.PushTokenDTO;
import dk.digitalidentity.os2faktor.api.dto.RegisterRequest;
import dk.digitalidentity.os2faktor.api.dto.RegisterResponse;
import dk.digitalidentity.os2faktor.api.dto.SetPinRequest;
import dk.digitalidentity.os2faktor.api.dto.ValidatePinRequest;
import dk.digitalidentity.os2faktor.api.model.PinResult;
import dk.digitalidentity.os2faktor.api.model.PinResultStatus;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
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

	@Value("${os2faktor.backend.feature.blocked.clients.enable:false}")
	private boolean blockedClientFeatureEnabled;

	@GetMapping("/api/client/v2/status")
	public ResponseEntity<ClientStatus> getStatus(@RequestHeader("deviceId") String deviceId, @RequestHeader(name = "uniqueClientId", required = false, defaultValue = "") String uniqueClientId) {
		ClientStatus response = new ClientStatus();
		boolean modified = false;
		
		Client client = clientService.getByDeviceId(deviceId);
		if (client != null) {
			response.setDisabled(client.isDisabled());
			response.setPinProtected(StringUtils.hasLength(client.getPincode()));
			response.setNemIdRegistered(client.getUser() != null);
			
			// if no value to compare against was supplied, we are talking about older software, and we will not perform the check
			if (StringUtils.hasLength(uniqueClientId)) {

				// if a value is stored already, compare against it, otherwise store the incoming value
				if (blockedClientFeatureEnabled) {
					if (StringUtils.hasLength(client.getUniqueClientId())) {
						response.setBlocked(!Objects.equals(client.getUniqueClientId(), uniqueClientId));
					}
					else {
						client.setUniqueClientId(uniqueClientId);
						modified = true;
					}
				}
				else {
					// when not enabled, we allow continued updating of clientID
					if (!Objects.equals(client.getUniqueClientId(), uniqueClientId)) {
						client.setUniqueClientId(uniqueClientId);
						modified = true;
					}
				}
			}
			
			if (!client.isDisabled()) {
				client.setLastUsed(new Date());
				modified = true;
			}
		}

		if (modified) {
			clientService.save(client);
		}
		
		if (response.isBlocked()) {
			log.warn("Returned BLOCKED status for deviceId = " + client.getDeviceId());
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
		
		if (request.getName() == null || request.getName().length() < 2 || request.getName().length() > 255) {
			log.warn("Invalid name '" + request.getName() + "'");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		Client client = new Client();
		client.setUseCount(0);
		client.setDeviceId(idGenerator.generateDeviceId());
		client.setName(request.getName());
		client.setToken(request.getToken());
		client.setType(request.getType());
		client.setClientVersion(clientVersion);
		client.setNsisLevel(NSISLevel.NONE);

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

		if (StringUtils.hasLength(client.getToken())) {
			if (client.getType().equals(ClientType.EDGE) || (client.getType().equals(ClientType.CHROME) && isJSONValid(client.getToken()))) {
				;
			}
			else {
				try {
					String notificationKey = snsService.createEndpoint(client.getToken(), client.getDeviceId(), client.getType());
					if (StringUtils.hasLength(notificationKey)) {
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

		if (request.isPasswordless() && request.getPincode() != null && request.getPincode().length() >= 6) {
			switch (client.getType()) {
				case ANDROID:
				case IOS:
					client.setPasswordless(true);
					break;
				default:
					log.warn("Attempting to set passwordless when registering client of invalid type: " + client.getDeviceId() + " / " + client.getType());
					break;
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
					c.add(Calendar.MINUTE, 5);
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
		if (StringUtils.hasLength(clientVersion)) {
			client.setClientVersion(clientVersion);

			try {
				client.setPincode(hashingService.encryptAndEncodeString(request.getNewPin()));
			}
			catch (Exception ex) {
				log.error("Failed to encrypt and encode pin", ex);

				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		// at least 6 character pins allow for passwordless on iOS and Android
		if (request.getNewPin().length() >= 6) {
			if (client.getType().equals(ClientType.ANDROID) || client.getType().equals(ClientType.IOS)) {
				client.setPasswordless(true);
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
		if (StringUtils.hasLength(clientVersion)) {
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
				c.add(Calendar.MINUTE, 5);
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
	
	@PostMapping("/api/client/v2/setRegId")
	public ResponseEntity<PinResult> setRegId(@RequestHeader("deviceId") String deviceId, @RequestHeader("clientVersion") String clientVersion, @RequestBody PushTokenDTO token) {
		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		if (client.isLocked()) {
			log.warn("Client with deviceId: " + client.getDeviceId() + " tried to set a push token (regId) while being locked" );
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		// update clientVersion and reg id
		if (StringUtils.hasLength(clientVersion)) {
			client.setClientVersion(clientVersion);
		}
		
		if (StringUtils.hasLength(token.getToken()) && !Objects.equals(token.getToken(), client.getToken())) {
			client.setToken(token.getToken());
	
			if (client.getType().equals(ClientType.EDGE) || (client.getType().equals(ClientType.CHROME) && isJSONValid(token.getToken()))) {
				;
			}
			else {
				try {
					String notificationKey = snsService.createEndpoint(token.getToken(), client.getDeviceId(), client.getType());
					if (StringUtils.hasLength(notificationKey)) {
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
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	public boolean isValidPin(String pinString) {
		// already checked, but let's avoid out of bound issues
		if (pinString.length() < 4 || pinString.length() > 6) {
			return false;
		}

		// only verify the first 4 digits - as pincodes of size 4 IS allowed

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
	
	private boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		}
		catch (Exception ex) {
			try {
				new JSONArray(test);	
			}
			catch (Exception ex1) {
				return false;
			}
		}
		
		return true;
	}
}
