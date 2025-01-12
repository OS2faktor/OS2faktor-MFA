package dk.digitalidentity.os2faktor.api;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.api.dto.AuthenticateUserRequestBody;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.ExternalLoginSession;
import dk.digitalidentity.os2faktor.dao.model.HardwareToken;
import dk.digitalidentity.os2faktor.dao.model.LoginServiceProvider;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import dk.digitalidentity.os2faktor.security.AuthorizedLoginServiceProviderHolder;
import dk.digitalidentity.os2faktor.service.ClientService;
import dk.digitalidentity.os2faktor.service.ExternalLoginSessionService;
import dk.digitalidentity.os2faktor.service.HardwareTokenService;
import dk.digitalidentity.os2faktor.service.HashingService;
import dk.digitalidentity.os2faktor.service.IdGenerator;
import dk.digitalidentity.os2faktor.service.SsnService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
public class LoginApiController {

	@Autowired
	private ClientService clientService;

	@Autowired
	private SsnService ssnService;

	@Autowired
	private ExternalLoginSessionService externalLoginSessionService;
	
	@Autowired
	private HardwareTokenService hardwareTokenService;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private HashingService hashingService;

	@Value("${os2faktor.frontend.baseurl}")
	private String frontendBaseUrl;

	@PostMapping("/api/login/authenticateUser")
	public ResponseEntity<?> authenticateUser(@RequestBody AuthenticateUserRequestBody body, @RequestParam(defaultValue = "yubikey") String type) throws Exception {
		// should not happen, but better safe than NullPointer ;)
		LoginServiceProvider loginServiceProvider = AuthorizedLoginServiceProviderHolder.getLoginServiceProvider();
		if (loginServiceProvider == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		
		if (!"yubikey".equals(type) && !"authenticator".equals(type) && !"kodeviser".equals(type)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		String sessionKey = UUID.randomUUID().toString();
		String encodedAndEncryptedSsn = ssnService.encryptAndEncodeSsn(body.getCpr());

		Calendar c1 = Calendar.getInstance();
		c1.setTime(new Date());
		c1.add(Calendar.MINUTE, 5);
		Date inFiveMinutes = c1.getTime();

		ExternalLoginSession externalLoginSession = new ExternalLoginSession();
		externalLoginSession.setLoginServiceProvider(loginServiceProvider);
		externalLoginSession.setNsisLevel(body.getNsisLevel());
		externalLoginSession.setSessionKey(sessionKey);
		externalLoginSession.setSsn(encodedAndEncryptedSsn);
		externalLoginSession.setTts(inFiveMinutes);

		externalLoginSessionService.save(externalLoginSession);
		
		return ResponseEntity.ok(frontendBaseUrl + "/external/" + type + "Handoff/" + sessionKey);
	}
	
	record RenameClientRequest(String deviceId, String name) { };
	
	@PostMapping("/api/login/renameClient")
	public ResponseEntity<?> renameClient(@RequestBody RenameClientRequest request) {
		// should not happen, but better safe than NullPointer ;)
		LoginServiceProvider loginServiceProvider = AuthorizedLoginServiceProviderHolder.getLoginServiceProvider();
		if (loginServiceProvider == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		
		if (request.name == null || request.name.length() == 0 || request.name.length() > 255) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);			
		}

		Client client = clientService.getByDeviceId(request.deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().build();
		}

		client.setName(request.name);
		clientService.save(client);
		
		log.info("LoginServiceProvider " + loginServiceProvider.getName() + " / " + loginServiceProvider.getCvr() + " has renamed client with deviceId " + request.deviceId + " to " + request.name);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/api/login/disableClient/{deviceId}")
	public ResponseEntity<?> disableClient(@PathVariable("deviceId") String deviceId) {
		// should not happen, but better safe than NullPointer ;)
		LoginServiceProvider loginServiceProvider = AuthorizedLoginServiceProviderHolder.getLoginServiceProvider();
		if (loginServiceProvider == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().build();
		}

		client.setDisabled(true);
		clientService.save(client);
		
		if (client.getType().equals(ClientType.TOTPH)) {
			HardwareToken token = hardwareTokenService.getByClient(deviceId);
			if (token != null && Objects.equals(token.getRegisteredToCvr(), loginServiceProvider.getCvr())) {
				token.setRegistered(false);
				token.setRegisteredToCpr(null);
				token.setRegisteredToCvr(null);
				token.setClientDeviceId(null);
				
				hardwareTokenService.save(token);
			}
		}

		log.info("LoginServiceProvider " + loginServiceProvider.getName() + " / " + loginServiceProvider.getCvr() + " has disabled client with deviceId " + deviceId);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping("/api/login/primaryClient/{deviceId}/{setPrime}")
	public ResponseEntity<?> setPrimaryClient(@PathVariable("deviceId") String deviceId, @PathVariable("setPrime") boolean setPrime) {
		// should not happen, but better safe than NullPointer ;)
		LoginServiceProvider loginServiceProvider = AuthorizedLoginServiceProviderHolder.getLoginServiceProvider();
		if (loginServiceProvider == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().build();
		}

		if (setPrime) {
			if (client.getUser() != null) {
				for (Client c : client.getUser().getClients()) {
					c.setPrime(Objects.equals(c.getDeviceId(), client.getDeviceId()));
				}

				clientService.saveAll(client.getUser().getClients());
			}
			else {
				client.setPrime(true);

				clientService.save(client);
			}
		}
		else {
			client.setPrime(false);

			clientService.save(client);
		}

		clientService.save(client);

		log.info("LoginServiceProvider " + loginServiceProvider.getName() + " / " + loginServiceProvider.getCvr() + " has set client with deviceId " + deviceId + " to primary");

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	private record RobotClientRequest(String name) { };
	private record RobotClientResponse(String secret, String deviceId) { };

	@PostMapping("/api/login/robo/register")
	public ResponseEntity<?> setRobotClient(@RequestBody RobotClientRequest roboClient) {
		LoginServiceProvider loginServiceProvider = AuthorizedLoginServiceProviderHolder.getLoginServiceProvider();
		if (loginServiceProvider == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		if (!StringUtils.hasText(roboClient.name)) {
			return new ResponseEntity<>("Name is missing", HttpStatus.BAD_REQUEST);
		}

		loginServiceProvider.getCvr();
		
		Client client = new Client();
		client.setDeviceId(idGenerator.generateDeviceId());
		client.setCreated(Timestamp.valueOf(LocalDateTime.now()));
		client.setType(ClientType.TOTP);
		client.setName(roboClient.name);
		client.setSecret(Base32.random());
		client.setNsisLevel(NSISLevel.NONE);
		client.setRobotMFA(true);

		String nonencryptedApiKey = idGenerator.generateUuid();
		try {
			client.setApiKey(hashingService.encryptAndEncodeString(nonencryptedApiKey));
		}
		catch (Exception ex) {
			log.error("Failed to encrypt and encode apiKey", ex);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		clientService.save(client);
		
		RobotClientResponse response = new RobotClientResponse(client.getSecret(), client.getDeviceId());
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
