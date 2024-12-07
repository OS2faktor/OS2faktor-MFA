package dk.digitalidentity.os2faktor.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;

public interface NotificationDao extends JpaRepository<Notification, String> {
	Notification findBySubscriptionKey(String subscriptionKey);
	Notification findByPollingKey(String subscriptionKey);
	
	List<Notification> findByClientAndClientRejectedFalseAndClientAuthenticatedFalseAndClientLockedFalse(Client client);
	List<Notification> findByClientNotifiedFalseAndClientType(ClientType clientType);
	List<Notification> findByCreatedBefore(Date timestamp);
	List<Notification> findByCreatedAfterAndClientType(Date timestamp, ClientType clientType);
	
	@Modifying
	@Query(nativeQuery = true, value = "UPDATE notifications SET client_notified = 0 WHERE client_device_id = ?1")
	void resetNotificationsForClient(String clientId);
}
