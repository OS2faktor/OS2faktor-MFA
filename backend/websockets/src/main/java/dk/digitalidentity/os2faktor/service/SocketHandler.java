package dk.digitalidentity.os2faktor.service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.os2faktor.api.model.PinResult;
import dk.digitalidentity.os2faktor.api.model.PinResultStatus;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.service.model.ClientSession;
import dk.digitalidentity.os2faktor.service.model.CustomWebSocketPayload;
import dk.digitalidentity.os2faktor.service.model.WSMessageDTO;
import dk.digitalidentity.os2faktor.service.model.WSMessageType;
import dk.digitalidentity.os2faktor.service.model.enums.MessageType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SocketHandler extends TextWebSocketHandler {
	private CopyOnWriteArrayList<ClientSession> sessions = new CopyOnWriteArrayList<>();

	@Autowired
	private ClientDao clientDao;
	
	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private HashingService hashingService;

	@Autowired
	private NotificationDao notificationDao;

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws InterruptedException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<?, ?> value = mapper.readValue(message.getPayload(), Map.class);

		String status = value.containsKey("status") ? value.get("status").toString() : "";
		String subscriptionKey = value.containsKey("subscriptionKey") ? value.get("subscriptionKey").toString() : "";
		String version = value.containsKey("version") ? value.get("version").toString() : "";
		String pinCode = value.containsKey("pin") ? value.get("pin").toString() : null;

		Optional<ClientSession> clientSession = sessions.stream().filter(cs -> cs.getSession().equals(session)).findAny();
		if (clientSession.isPresent()) {
			// could simply be an "is-alive" check
			switch (status) {
				case "ISALIVE":
					clientSession.get().setCleanupTimestamp(null);
					return;
				case "ACCEPT":
				case "REJECT":
					Notification challenge = notificationDao.getBySubscriptionKey(subscriptionKey);
					if (challenge == null) {
						log.warn("Trying to answer a challenge that does not exist: " + subscriptionKey);
						return;
					}
					
					Client client = clientDao.getByDeviceId(clientSession.get().getClient().getDeviceId());
					if ("ACCEPT".equals(status)) {
						PinResult result = new PinResult();
						
						if (client.isLocked()) {
							log.warn("Client with deviceId: " + client.getDeviceId() + " tried to accept a challenge while being locked out." );
						
							result.setStatus(PinResultStatus.LOCKED);
							SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
							result.setLockedUntil(format.format(client.getLockedUntil()));
							
							sendIncorrectPinMessage(client, subscriptionKey, result);
							return;
						}

						if (challenge.getClient().getPincode() != null) {
							boolean matches = false;
							try {
								matches = hashingService.matches(pinCode, challenge.getClient().getPincode());
							} catch (Exception ex) {
								log.error("Failed to check matching pincode", ex);
								return;
							}

							if (!matches) {
								log.warn("Wrong pin for device: " + challenge.getClient().getDeviceId());

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
								sendIncorrectPinMessage(client, subscriptionKey, result);
								return;
							}
						}

						client.setFailedPinAttempts(0);
						sendCorrectPinMessage(client, subscriptionKey);
						challenge.setClientAuthenticated(true);
						challenge.setClientResponseTimestamp(new Date());
					}
					else if ("REJECT".equals(status)) {
						challenge.setClientRejected(true);
						challenge.setClientResponseTimestamp(new Date());
					}

					notificationDao.save(challenge);

					client.setUseCount(client.getUseCount() + 1);
					client.setClientVersion(version);
					clientDao.save(client);

					break;
				default:
					log.warn("Client called with status = " + status + "  which is an unknown status type!");
					break;
			}
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		String apiKey = extractApiKeyFromSession(session);
		if (apiKey == null) {
			log.debug("No suitable ApiKey supplied by client");
			session.close(CloseStatus.NOT_ACCEPTABLE);
			return;
		}

		// TODO: once all windows 10 clients are updated to send username/password as deviceId/apiKey, we can
		//       extract the deviceId below (right now it returns "apiKey" as the username because that was
		//       how the client was originally implemented
		String encodedString = hashingService.encryptAndEncodeString(apiKey);
		Client client = clientDao.getByApiKey(encodedString);
		if (client != null) {
			sessions.add(new ClientSession(client, session));
		}
		else {
			session.close(CloseStatus.POLICY_VIOLATION.withReason("Client does not exist"));
		}
	}

	private String extractApiKeyFromSession(WebSocketSession session) {
		List<String> authorizationList = session.getHandshakeHeaders().get("Authorization");

		if (authorizationList != null && authorizationList.size() > 0) {
			String authorization = authorizationList.get(0);
			String base64Credentials = authorization.substring("Basic".length()).trim();

			try {
				String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.forName("UTF-8"));
				String[] values = credentials.split(":", 2);

				return values[1];
			}
			catch (Exception ex) {
				log.error("Error occured while trying to decode Authorization header", ex);
			}
		}

		return null;
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		sessions.removeIf(s -> s.getSession().equals(session));
	}
	
	private void sendCorrectPinMessage(Client client, String subscriptionKey) {
		Optional<ClientSession> clientSession = sessions.stream()
				.filter(cs -> cs.getClient().getDeviceId().equals(client.getDeviceId()))
				.findAny();

		if (clientSession.isPresent()) {
			ObjectMapper mapper = new ObjectMapper();

			WSMessageDTO jsonObject = new WSMessageDTO();
			jsonObject.setMessageType(WSMessageType.CORRECT_PIN);
			jsonObject.setSubscriptionKey(subscriptionKey);
			
			String data = null;
			try {
				data = mapper.writeValueAsString(jsonObject);
			}
			catch (JsonProcessingException ex) {
				log.error("Cannot parse to JSON", ex);
				return;
			}

			CustomWebSocketPayload<?> payload = new CustomWebSocketPayload<String>(UUID.randomUUID().toString(), MessageType.JSON, data, true);
			TextMessage message = new TextMessage(payload.toJSONString());
			try {
				clientSession.get().getSession().sendMessage(message);
			}
			catch (IOException e) {
				log.error("Error occured while sending message through WebSocket: " + e.getMessage());
			}
		}
	}
	
	public void sendIncorrectPinMessage(Client client, String subscriptionKey, PinResult result) {
		Optional<ClientSession> clientSession = sessions.stream()
				.filter(cs -> cs.getClient().getDeviceId().equals(client.getDeviceId()))
				.findAny();

		if (clientSession.isPresent()) {
			ObjectMapper mapper = new ObjectMapper();

			WSMessageDTO jsonObject = new WSMessageDTO();
			jsonObject.setMessageType(WSMessageType.INCORRECT_PIN);
			jsonObject.setSubscriptionKey(subscriptionKey);
			jsonObject.setPinResult(result);
			
			String data = null;
			try {
				data = mapper.writeValueAsString(jsonObject);
			}
			catch (JsonProcessingException ex) {
				log.error("Cannot parse to JSON", ex);
				return;
			}

			CustomWebSocketPayload<?> payload = new CustomWebSocketPayload<String>(UUID.randomUUID().toString(), MessageType.JSON, data, true);
			TextMessage message = new TextMessage(payload.toJSONString());
			try {
				clientSession.get().getSession().sendMessage(message);
			}
			catch (IOException e) {
				log.error("Error occured while sending message through WebSocket: " + e.getMessage());
			}
		}
	}

	public void closeStaleSessions() {
		Date fiveMinutesAgo = new Date(System.currentTimeMillis() - (5 * 60 * 1000));

		for (ClientSession clientSession : sessions) {
			// supported since version 1.4.0
			if (getMajorClientVersion(clientSession) >= 2 || (getMajorClientVersion(clientSession) == 1 && getMinorClientVersion(clientSession) >= 4)) {
				if (clientSession.getCleanupTimestamp() != null) {
					if (fiveMinutesAgo.after(clientSession.getCleanupTimestamp())) {
						try {
							log.info("Closing stale connection on: " + clientSession.getClient().getDeviceId());
							clientSession.getSession().close();
						}
						catch (Exception ex) {
							log.warn("Failed to close connection for " + clientSession.getClient().getDeviceId() + ": " + ex.getMessage());
						}
					}
				}
				else {
					ObjectMapper mapper = new ObjectMapper();
	
					WSMessageDTO jsonObject = new WSMessageDTO();
					jsonObject.setMessageType(WSMessageType.KEEP_ALIVE);
					
					String data = null;
					try {
						data = mapper.writeValueAsString(jsonObject);
					}
					catch (JsonProcessingException ex) {
						log.error("Cannot parse to JSON", ex);
						return;
					}
	
					CustomWebSocketPayload<?> payload = new CustomWebSocketPayload<String>(UUID.randomUUID().toString(), MessageType.NOTIFICATION, data, true);
					TextMessage message = new TextMessage(payload.toJSONString());
	
					try {
						if (log.isDebugEnabled()) {
							log.debug("Sending cleanup message: " + message + " to " + clientSession.getClient().getDeviceId());
						}
	
						clientSession.getSession().sendMessage(message);
						clientSession.setCleanupTimestamp(new Date());
					}
					catch (IllegalStateException e) {
						log.warn("failed to send message: " + e.getMessage());

						try {
							clientSession.getSession().close();
						}
						catch (Exception ex) {
							; // ignore
						}
					}
					catch (IOException e) {
						log.error("Error occured while sending message through WebSocket: " + e.getMessage());
					}
				}
			}
		}
	}

	public void sendNotification(Client client, Notification challenge) {
		Optional<ClientSession> clientSession = sessions.stream()
				.filter(cs -> cs.getClient().getDeviceId().equals(client.getDeviceId()))
				.findAny();

		if (clientSession.isPresent()) {
			ObjectMapper mapper = new ObjectMapper();

			ClientSession session = clientSession.get();

			String challengeValue = challenge.getChallenge();
			if (!StringUtils.hasLength(challengeValue)) {
				// supported since version 2.1.x
				if (getMajorClientVersion(session) < 2 || (getMajorClientVersion(session) == 2 && getMinorClientVersion(session) < 1)) {
					// hack - no challenge actually exists, so we generate a random one
					log.warn("Generate a random challenge for old windows client");
					challengeValue = idGenerator.generateChallenge();
				}
			}

			WSMessageDTO jsonObject = new WSMessageDTO();
			jsonObject.setMessageType(WSMessageType.NOTIFICATION);
			jsonObject.setSubscriptionKey(challenge.getSubscriptionKey());
			jsonObject.setChallenge(challengeValue);
			jsonObject.setServerName(challenge.getServerName());
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
			jsonObject.setTts("kl " + sdf.format(challenge.getCreated()));
			
			String data = null;
			try {
				data = mapper.writeValueAsString(jsonObject);
			}
			catch (JsonProcessingException ex) {
				log.error("Cannot parse to JSON", ex);
				return;
			}

			CustomWebSocketPayload<?> payload = new CustomWebSocketPayload<String>(UUID.randomUUID().toString(), MessageType.NOTIFICATION, data, true);
			TextMessage message = new TextMessage(payload.toJSONString());

			try {
				if (log.isDebugEnabled()) {
					log.debug("Sending message: " + message + " to " + client.getDeviceId());
				}

				clientSession.get().getSession().sendMessage(message);

				challenge.setClientNotified(true);
				challenge.setSentTimestamp(new Date());
				notificationDao.save(challenge);
			}
			catch (IOException e) {
				log.error("Error occured while sending message through WebSocket: " + e.getMessage());
			}
		}
		else {
			if (log.isDebugEnabled()) {
				log.debug("ClientSession not found for Client:" + client.getDeviceId());
			}
		}
	}
	
	private int getMinorClientVersion(ClientSession clientSession) {
		if (clientSession.getClient() != null && clientSession.getClient().getClientVersion() != null) {
			String[] tokens = clientSession.getClient().getClientVersion().split("\\.");
			if (tokens.length >= 2) {
				try {
					return Integer.parseInt(tokens[1]);
				}
				catch (Exception ex) {
					log.warn("Malformed client version '" + clientSession.getClient().getClientVersion() + "' for " + clientSession.getClient().getDeviceId());
				}
			}
		}

		return 0;
	}

	private int getMajorClientVersion(ClientSession clientSession) {
		if (clientSession.getClient() != null && clientSession.getClient().getClientVersion() != null) {
			String[] tokens = clientSession.getClient().getClientVersion().split("\\.");

			try {
				return Integer.parseInt(tokens[0]);
			}
			catch (Exception ex) {
				log.warn("Malformed client version '" + clientSession.getClient().getClientVersion() + "' for " + clientSession.getClient().getDeviceId());
			}
		}

		return 0;
	}
}