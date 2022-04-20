package dk.digitalidentity.os2faktor.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;

public interface NotificationDao extends JpaRepository<Notification, String> {
	Notification getBySubscriptionKey(String subscriptionKey);
	Notification getByPollingKey(String subscriptionKey);
	
	List<Notification> getByClientAndClientRejectedFalseAndClientAuthenticatedFalseAndClientLockedFalse(Client client);
	List<Notification> getByClientNotifiedFalseAndClientType(ClientType clientType);
	List<Notification> getByClientNotifiedTrueAndClientRejectedFalseAndClientAuthenticatedFalse();
	List<Notification> getByCreatedBefore(Date timestamp);
}
