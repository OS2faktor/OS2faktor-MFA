package dk.digitalidentity.os2faktor.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.Client;

public interface ClientDao extends JpaRepository<Client, String> {
	Client getByDeviceId(String deviceId);

	// use getByDeviceId instead - and then compare apiKey manually
	// we do not have an index on this field, so lookup will be slow if we use this
	@Deprecated
	Client getByApiKey(String apiKey);
	
	List<Client> getByNotificationKey(String notificationKey);

	List<Client> getByLockedTrue();

	void deleteByDisabledTrueAndLastUsedIsNullOrLastUsedBefore(Date timestamp);
}
