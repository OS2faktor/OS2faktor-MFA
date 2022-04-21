package dk.digitalidentity.os2faktor.api;

import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.api.model.PollingResponse;
import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.PseudonymDao;
import dk.digitalidentity.os2faktor.dao.ServerDao;
import dk.digitalidentity.os2faktor.dao.StatisticDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.LocalClient;
import dk.digitalidentity.os2faktor.dao.model.Municipality;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.Pseudonym;
import dk.digitalidentity.os2faktor.dao.model.Server;
import dk.digitalidentity.os2faktor.dao.model.Statistic;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.security.AuthorizedServerHolder;
import dk.digitalidentity.os2faktor.service.ClientService;
import dk.digitalidentity.os2faktor.service.IdGenerator;
import dk.digitalidentity.os2faktor.service.LocalClientService;
import dk.digitalidentity.os2faktor.service.PushNotificationSenderService;
import dk.digitalidentity.os2faktor.service.UserService;
import dk.digitalidentity.os2faktor.service.model.PushStatus;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Subscription.Keys;

@Slf4j
@CrossOrigin
@RestController
public class ServerApi {
	
	@Autowired
	private ClientService clientService;
	
	@Autowired
	private ServerDao serverDao;

	@Autowired
	private PseudonymDao pseudonymDao;

	@Autowired
	private NotificationDao subscriptionInfoDao;
	
	@Autowired
	private PushNotificationSenderService notificationService;
	
	@Autowired
	private LocalClientService localClientService;

	@Autowired
	private UserService userService;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private StatisticDao statisticDao;
	
	@Autowired
	private PushService pushService;
	
	@Value("${os2faktor.frontend.baseurl}")
	private String frontendBaseUrl;

	@GetMapping("/api/server/clients")
	public ResponseEntity<?> getClients(
			@RequestParam(required = false, value = "ssn") String encodedSsn,
			@RequestParam(required = false, value = "pid") String pid,
			@RequestParam(required = false, value = "pseudonym") String pseudonym,
			@RequestParam(required = false, value = "deviceId") String[] deviceIds,
			@RequestHeader("connectorVersion") String connectorVersion) {

		// should not happen, but better safe than NullPointer ;)
		Server server = AuthorizedServerHolder.getServer();
		if (server == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		
		if (server.getConnectorVersion() == null && connectorVersion != null || !server.getConnectorVersion().equals(connectorVersion)) {
			if (connectorVersion.length() > 32) {
				return new ResponseEntity<>("ConnectorVersion header must not be longer than 32 characters!", HttpStatus.BAD_REQUEST);
			}

			server.setConnectorVersion(connectorVersion);
			serverDao.save(server);
		}

		HashSet<Client> clients = new HashSet<>();

		try {
			if (encodedSsn != null && encodedSsn.length() > 0) {
				addUsersClientsByEncodedSsn(encodedSsn, server.getMunicipality().getCvr(), clients);
			}
			else if (pid != null && pid.length() > 0) {
				User user = userService.getByPid(pid);
	
				if (user != null && user.getClients() != null && user.getClients().size() > 0) {
					clients.addAll(user.getClients());
				}
			}
			else if (pseudonym != null && pseudonym.length() > 0) {
				Municipality municipality = AuthorizedServerHolder.getServer().getMunicipality();
				if (municipality == null) {
					log.warn("No municipality assigned to Server:" + server.getName());
	
					return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
				}
	
				Pseudonym pseudonymMapping = pseudonymDao.getByPseudonymAndCvr(pseudonym, municipality.getCvr());
				if (pseudonymMapping != null) {
					String encryptedAndEncodedSsn = pseudonymMapping.getSsn();
	
					addUsersClientsByEncryptedAndEncodedSsn(encryptedAndEncodedSsn, clients);
				}
			}
	
			if (deviceIds != null && deviceIds.length > 0) {
				for (String deviceId : deviceIds) {
					Client client = clientService.getByDeviceId(deviceId);
	
					if (client != null) {
						clients.add(client);
					}
				}
			}
			
			// filter out the disabled and locked clients
			clients.removeIf(c -> c.isDisabled());
			clients.removeIf(c -> c.isLocked());
	
			// inform Connector about pincode status for this client
			for (Client client : clients) {
				if (client.getPincode() != null && client.getPincode().length() > 0) {
					client.setHasPincode(true);
				}
				else if (client.getType().equals(ClientType.YUBIKEY)) {
					// for now the decision is that YubiKeys have a pincode build in due to its physical nature
					client.setHasPincode(true);
				}
				else if (client.getType().equals(ClientType.TOTP)) {
					// not really pincode protected - but we will default to say it has, as TOTP clients can be
					// rejected by the customer by type if they do not approve the use of these
					client.setHasPincode(true);
				}
			}
		}
		catch (Exception ex) {
			log.error("Bad request from server " + server.getName(), ex);
			return new ResponseEntity<>("Unexpected error processing payload", HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(clients, HttpStatus.OK);
	}
	
	@PutMapping("/api/server/client/{deviceId}/authenticate")
	public ResponseEntity<Notification> authenticateClient(@PathVariable("deviceId") String deviceId, @RequestParam(value = "emitChallenge", defaultValue = "true", required = false) boolean emitChallenge) {
		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (client.isDisabled()) {
			log.warn("Tried to challenge a disabled Client: " + client.getDeviceId());

			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		if (client.isLocked()) {
			log.warn("Tried to challenge a locked Client: " + client.getDeviceId());

			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Server server = AuthorizedServerHolder.getServer();
		if (server == null) {
			log.warn("Access to API blocked, because server is not authorized!");
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Notification subscriptionInfo = new Notification();
		subscriptionInfo.setSubscriptionKey(idGenerator.generateUuid());
		subscriptionInfo.setPollingKey(idGenerator.generateUuid());
		subscriptionInfo.setClientNotified(false);
		subscriptionInfo.setClientAuthenticated(false);
		subscriptionInfo.setClientRejected(false);
		subscriptionInfo.setClient(client);
		subscriptionInfo.setServerName(server.getName());
		subscriptionInfo.setServerId(server.getId());
		subscriptionInfo.setChallenge((emitChallenge) ? idGenerator.generateChallenge() : "");

		if (client.getType().equals(ClientType.EDGE)) {
			String token = client.getToken();
			if (token != null && token.length() > 0) {
				try {
					JSONObject obj = new JSONObject(token);
					String endpoint = obj.getString("endpoint");
					String key = obj.getJSONObject("keys").getString("p256dh");
					String auth = obj.getJSONObject("keys").getString("auth");
					String json = "{\"title\": \"OS2faktor\", \"body\": \"Login forsøg\"}";

					pushService.send(new nl.martijndwars.webpush.Notification(new Subscription(endpoint, new Keys(key, auth)), json));
					subscriptionInfo.setClientNotified(true);
					subscriptionInfo.setSentTimestamp(new Date());
				}
				catch (Exception ex) {
					log.error("Failed to send push notification to edge client: " + client.getDeviceId(), ex);
				}
			}
		}
		else if (client.getType().equals(ClientType.ANDROID) || client.getType().equals(ClientType.IOS) || client.getType().equals(ClientType.CHROME)) {
			if (client.getNotificationKey() != null && client.getNotificationKey().length() > 0) {
				subscriptionInfo.setClientNotified(true);
				subscriptionInfo.setSentTimestamp(new Date());
				PushStatus status = notificationService.publish("Login forsøg", client.getNotificationKey());
				if (status.equals(PushStatus.DISABLED)) {
					client.setDisabled(true);
					clientService.save(client);
					
					return new ResponseEntity<>(HttpStatus.NOT_FOUND);
				}
			}
		}
		else if (client.getType().equals(ClientType.YUBIKEY)) {
			subscriptionInfo.setRedirectUrl(frontendBaseUrl + "/mfalogin/yubikey/" + subscriptionInfo.getPollingKey());
			subscriptionInfo.setChallenge(Base64.getEncoder().encodeToString(idGenerator.getRandomBytes(32)));
		}
		else if (client.getType().equals(ClientType.TOTP)) {
			subscriptionInfo.setRedirectUrl(frontendBaseUrl + "/mfalogin/authenticator/" + subscriptionInfo.getPollingKey());
		}

		subscriptionInfoDao.save(subscriptionInfo);
		
		Statistic statistic = new Statistic();
		statistic.setClientType(client.getType().toString());
		statistic.setClientVersion(client.getClientVersion());
		statistic.setCvr(server.getMunicipality().getCvr());
		statistic.setDeviceId(client.getDeviceId());
		statistic.setServerId(server.getId());
		statisticDao.save(statistic);

		// not super important to take care of race-conditions here, as long as we have a rough count
		server.setUseCount(server.getUseCount() + 1);
		serverDao.save(server);

		return new ResponseEntity<>(subscriptionInfo, HttpStatus.OK);
	}
	
	// query result for specific notification send previously to client
	@GetMapping("/api/server/notification/{subscriptionKey}/status")
	public ResponseEntity<Notification> getSubscriptionStatus(@PathVariable("subscriptionKey") String subscriptionKey) {
		Notification subscriptionInfo = subscriptionInfoDao.getBySubscriptionKey(subscriptionKey);
		if (subscriptionInfo == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(subscriptionInfo, HttpStatus.OK);
	}

	// does not require authorization (bypasses SecurityFilter because of url-mapping)
	@GetMapping("/api/notification/{pollingKey}/poll")
	public ResponseEntity<PollingResponse> pollSubscription(@PathVariable("pollingKey") String pollingKey) {
		PollingResponse response = new PollingResponse();
		response.setStateChange(false);
		
		Notification subscription = subscriptionInfoDao.getByPollingKey(pollingKey);
		if (subscription == null || subscription.isClientAuthenticated() || subscription.isClientRejected()) {
			response.setStateChange(true);
		}
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private void addUsersClientsByEncodedSsn(String encodedSsn, String cvr, HashSet<Client> clients) {
		User user = userService.getByEncodedSsn(encodedSsn);

		if (user != null && user.getClients() != null && user.getClients().size() > 0) {
			clients.addAll(user.getClients());
		}
		
		List<LocalClient> localClients = localClientService.getByEncodedSsnAndCvr(encodedSsn, cvr);
		if (localClients != null && localClients.size() > 0) {
			for (LocalClient localClient : localClients) {
				Client client = clientService.getByDeviceId(localClient.getDeviceId());

				if (client != null) {
					clients.add(client);
				}
			}
		}
	}
	
	private void addUsersClientsByEncryptedAndEncodedSsn(String encryptedAndEncodedSsn, HashSet<Client> clients) {
		User user = userService.getByEncryptedAndEncodedSsn(encryptedAndEncodedSsn);

		if (user != null && user.getClients() != null && user.getClients().size() > 0) {
			clients.addAll(user.getClients());
		}
	}
}
