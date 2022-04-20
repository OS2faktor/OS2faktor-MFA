package dk.digitalidentity.os2faktor.api;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.api.dto.AuthenticateUserRequestBody;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.ExternalLoginSession;
import dk.digitalidentity.os2faktor.dao.model.LoginServiceProvider;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.security.AuthorizedLoginServiceProviderHolder;
import dk.digitalidentity.os2faktor.service.ExternalLoginSessionService;
import dk.digitalidentity.os2faktor.service.SsnService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
public class LoginApiController {

	@Autowired
	private ClientDao clientDao;

	@Autowired
	private SsnService ssnService;

	@Autowired
	private ExternalLoginSessionService externalLoginSessionService;

	@Value("${os2faktor.frontend.baseurl}")
	private String frontendBaseUrl;

	@PostMapping("/api/login/authenticateUser")
	public ResponseEntity<?> authenticateUser(@RequestBody AuthenticateUserRequestBody body) throws Exception {
		// should not happen, but better safe than NullPointer ;)
		LoginServiceProvider loginServiceProvider = AuthorizedLoginServiceProviderHolder.getLoginServiceProvider();
		if (loginServiceProvider == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
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

		return ResponseEntity.ok(frontendBaseUrl + "/external/yubikeyHandoff/" + sessionKey);
	}

	@PostMapping("/api/login/disableClient/{deviceId}")
	public ResponseEntity<?> disableClient(@PathVariable("deviceId") String deviceId) {
		// should not happen, but better safe than NullPointer ;)
		LoginServiceProvider loginServiceProvider = AuthorizedLoginServiceProviderHolder.getLoginServiceProvider();
		if (loginServiceProvider == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		Client client = clientDao.getByDeviceId(deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().build();
		}

		client.setDisabled(true);
		clientDao.save(client);

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

		Client client = clientDao.getByDeviceId(deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().build();
		}
		
		if (client.getType() == ClientType.YUBIKEY) {
			return new ResponseEntity<>("Client of type " + ClientType.YUBIKEY + " cannot be selected as prime.", HttpStatus.BAD_REQUEST);
		}
		
		if (setPrime) {
			if (client.getUser() != null) {
				for (Client c : client.getUser().getClients()) {
					c.setPrime(Objects.equals(c.getDeviceId(), client.getDeviceId()));
				}

				clientDao.saveAll(client.getUser().getClients());
			}
			else {
				client.setPrime(true);

				clientDao.save(client);
			}
		}
		else {
			client.setPrime(false);

			clientDao.save(client);
		}

		clientDao.save(client);

		log.info("LoginServiceProvider " + loginServiceProvider.getName() + " / " + loginServiceProvider.getCvr() + " has set client with deviceId " + deviceId + " to primary");

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
