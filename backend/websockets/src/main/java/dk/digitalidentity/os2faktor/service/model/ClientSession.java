package dk.digitalidentity.os2faktor.service.model;

import java.util.Date;

import org.springframework.web.socket.WebSocketSession;

import dk.digitalidentity.os2faktor.dao.model.Client;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientSession {
	private String clientDeviceId;
	private String clientVersion;
	private WebSocketSession session;
	private Date cleanupTimestamp;

	public ClientSession(Client client, WebSocketSession session) {
		this.session = session;
		this.clientDeviceId = client.getDeviceId();
		this.clientVersion = client.getClientVersion();
		this.cleanupTimestamp = null;
	}
}
