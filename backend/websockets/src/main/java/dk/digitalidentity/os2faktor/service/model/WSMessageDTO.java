package dk.digitalidentity.os2faktor.service.model;

import dk.digitalidentity.os2faktor.api.model.PinResult;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WSMessageDTO {
	private WSMessageType messageType;
	private String subscriptionKey;
	private String challenge;
	private String serverName;
	private PinResult pinResult;
}
