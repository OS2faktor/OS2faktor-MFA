package dk.digitalidentity.os2faktor.service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.os2faktor.api.model.PinResult;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.NotificationDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.service.model.ClientSession;
import dk.digitalidentity.os2faktor.service.model.CustomWebSocketPayload;
import dk.digitalidentity.os2faktor.service.model.WSMessageDTO;
import dk.digitalidentity.os2faktor.service.model.WSMessageType;
import dk.digitalidentity.os2faktor.service.model.enums.MessageType;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SocketHandler extends TextWebSocketHandler {
	private CopyOnWriteArrayList<ClientSession> sessions = new CopyOnWriteArrayList<>();

	@Autowired
	private ClientDao clientDao;

	@Autowired
	private NotificationDao notificationDao;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private HashingService hashingService;
	
	@Autowired
	private WebsocketClientService websocketClientService;
	
	@Autowired
	private SocketHandler self;
	
	@Value("${os2faktor.debug.traceConnections:false}")
	private boolean traceConnections;

	public int countConnections() {
		return sessions.size();
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws InterruptedException, IOException {

		ClientSession clientSession = sessions.stream().filter(cs -> cs.getSession().equals(session)).findAny().orElse(null);
		if (clientSession != null) {
			ObjectMapper mapper = new ObjectMapper();
			Map<?, ?> value = mapper.readValue(message.getPayload(), Map.class);

			String status = value.containsKey("status") ? value.get("status").toString() : "";
			String subscriptionKey = value.containsKey("subscriptionKey") ? value.get("subscriptionKey").toString() : "";
			String version = value.containsKey("version") ? value.get("version").toString() : "";
			String pinCode = value.containsKey("pin") ? value.get("pin").toString() : null;

			switch (status) {
				case "ISALIVE":
					clientSession.setCleanupTimestamp(null);
					break;
				case "ACCEPT":
				case "REJECT":
					PinResult pinResult = websocketClientService.handleAcceptReject(status, clientSession.getClientDeviceId(), subscriptionKey, version, pinCode);
					if (pinResult != null) {
						switch (pinResult.getStatus()) {
							case OK:
								sendCorrectPinMessage(clientSession, subscriptionKey);
								break;
							case LOCKED:
							case WRONG_PIN:
								sendIncorrectPinMessage(clientSession, subscriptionKey, pinResult);
								break;
							case INVALID_NEW_PIN:
								log.error("Unexpected return value from handleAcceptReject: INVALID_NEW_PIN");
								break;
						}
					}
					break;
				default:
					log.warn("Client called with status = " + status + "  which is an unknown status type!");
					break;
			}
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		try {
			self.processNewConnection(session);
		}
		catch (RequestNotPermitted ex) {
			log.warn("Rejecting connection due to service overload");
			session.close(CloseStatus.SERVICE_OVERLOAD.withReason("Service overload"));	
		}
	}
	
	@RateLimiter(name = "processNewConnections")
	public void processNewConnection(WebSocketSession session) throws Exception {
		Client client = null;
		
		String deviceId = extractDeviceIdFromSession(session);
		if (deviceId != null) {
			client = clientDao.findByDeviceId(deviceId);
		}

		if (client == null) {
			String apiKey = extractApiKeyFromSession(session);
			if (apiKey == null) {
				log.debug("No suitable ApiKey supplied by client");
				session.close(CloseStatus.NOT_ACCEPTABLE);
				return;
			}

			// fallback mechanism for ancient clients
			String encodedString = hashingService.encryptAndEncodeString(apiKey);
			client = clientDao.findByApiKey(encodedString);
		}
		
		if (client != null) {
			if (traceConnections) {
				log.info("Established connection from " + client.getDeviceId());
			}
			
			sessions.add(new ClientSession(client, session));
		}
		else {
			session.close(CloseStatus.POLICY_VIOLATION.withReason("Client does not exist"));
		}
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		ClientSession clientSession = sessions.stream().filter(s -> s.getSession().equals(session)).findFirst().orElse(null);
		boolean removed = sessions.removeIf(s -> s.getSession().equals(session));
		
		if (clientSession != null) {
			// Let us see if we get any of these .... I think session.equals() works as intended, but better safe than sorry
			if (removed == false) {
				log.error("sessions.removeIf() has a coding error....");
			}
			
			if (traceConnections) {
				log.info("Closed connection from " + clientSession.getClientDeviceId());
			}
		}
	}
	
	public void sendIncorrectPinMessage(ClientSession clientSession, String subscriptionKey, PinResult result) {
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
			clientSession.getSession().sendMessage(message);
		}
		catch (IOException e) {
			log.error("Error occured while sending message through WebSocket: " + e.getMessage());
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
							log.info("Closing stale connection on: " + clientSession.getClientDeviceId());
							clientSession.getSession().close();
						}
						catch (Exception ex) {
							log.warn("Failed to close connection for " + clientSession.getClientDeviceId() + ": " + ex.getMessage());
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
							log.debug("Sending cleanup message: " + message + " to " + clientSession.getClientDeviceId());
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
		StopWatch stopWatch = new StopWatch("sendNotification");
		stopWatch.start();

		Optional<ClientSession> clientSession = sessions.stream()
				.filter(cs -> Objects.equals(cs.getClientDeviceId(), client.getDeviceId()))
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

			if (challenge.isClientNotified()) {
				log.warn("Resending challenge to " + session.getClientDeviceId());
			}
			else {
				log.info("Sending challenge to " + session.getClientDeviceId());
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
				clientSession.get().getSession().sendMessage(message);

				challenge.setClientNotified(true);
				challenge.setSentTimestamp(new Date());
				notificationDao.save(challenge);
				
				stopWatch.stop();
				long time = stopWatch.getTotalTimeMillis();
				if (time > 1000) {
					log.warn("It took a while (" + time + " ms) to send a challenge to " + client.getDeviceId() + ". Details = " + stopWatch.prettyPrint());
				}
			}
			catch (IOException e) {
				log.error("Error occured while sending message through WebSocket: " + e.getMessage());
			}
		}
	}
	
	private int getMinorClientVersion(ClientSession clientSession) {
		if (clientSession.getClientVersion() != null) {
			String[] tokens = clientSession.getClientVersion().split("\\.");
			
			if (tokens.length >= 2) {
				try {
					return Integer.parseInt(tokens[1]);
				}
				catch (Exception ex) {
					log.warn("Malformed client version '" + clientSession.getClientVersion() + "' for " + clientSession.getClientDeviceId());
				}
			}
		}

		return 0;
	}

	private int getMajorClientVersion(ClientSession clientSession) {
		if (clientSession.getClientVersion() != null) {
			String[] tokens = clientSession.getClientVersion().split("\\.");

			try {
				return Integer.parseInt(tokens[0]);
			}
			catch (Exception ex) {
				log.warn("Malformed client version '" + clientSession.getClientVersion() + "' for " + clientSession.getClientDeviceId());
			}
		}

		return 0;
	}
	
	private String extractDeviceIdFromSession(WebSocketSession session) {
		List<String> authorizationList = session.getHandshakeHeaders().get("Authorization");

		if (authorizationList != null && authorizationList.size() > 0) {
			String authorization = authorizationList.get(0);
			String base64Credentials = authorization.substring("Basic".length()).trim();

			try {
				String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.forName("UTF-8"));
				String[] values = credentials.split(":", 2);

				String deviceId = values[0];
				
				// if we get 15 characters, it is very likely a deviceId
				if (deviceId.length() == 15) {
					return deviceId;
				}
			}
			catch (Exception ex) {
				log.error("Error occured while trying to decode Authorization header", ex);
			}
		}

		return null;
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
	
	private void sendCorrectPinMessage(ClientSession clientSession, String subscriptionKey) {
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
			clientSession.getSession().sendMessage(message);
		}
		catch (IOException e) {
			log.error("Error occured while sending message through WebSocket: " + e.getMessage());
		}
	}
}