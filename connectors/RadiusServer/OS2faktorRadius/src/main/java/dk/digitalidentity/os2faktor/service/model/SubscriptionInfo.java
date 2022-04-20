package dk.digitalidentity.os2faktor.service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionInfo {
	private String subscriptionKey;
	private String pollingKey;
	private boolean clientNotified;
	private boolean clientAuthenticated;
	private boolean clientRejected;
	private String challenge;
}
