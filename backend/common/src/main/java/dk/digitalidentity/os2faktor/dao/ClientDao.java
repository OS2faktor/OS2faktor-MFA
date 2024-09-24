package dk.digitalidentity.os2faktor.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dk.digitalidentity.os2faktor.dao.model.Client;

public interface ClientDao extends JpaRepository<Client, String> {
	Client findByDeviceId(String deviceId);

	// use getByDeviceId instead - and then compare apiKey manually
	// we do not have an index on this field, so lookup will be slow if we use this
	@Deprecated
	Client findByApiKey(String apiKey);

	Client findByYubikeyUid(String uid);
	
	List<Client> findByNotificationKey(String notificationKey);

	List<Client> findByLockedTrue();

	// delete 3 months after being disabled
	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM clients WHERE disabled = 1 AND (last_used IS NULL OR last_used < NOW() - INTERVAL 3 MONTH)")
	int deleteOldDisabledClients();

	// note - none of the two cleanup jobs below runs for TOTPH/YUBIKEY devices on purpose, as they represent a physical device, and we should not
	// remove them just because they are not being used
	
	// delete clients that has never been used 6 months after creation
	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM clients WHERE client_type NOT IN ('TOTPH', 'YUBIKEY') AND use_count = 0 AND created < NOW() - INTERVAL 6 MONTH")
	int deleteOldUnusedClients();

	// delete any client that has not been used for 12 months
	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM clients WHERE client_type NOT IN ('TOTPH', 'YUBIKEY') AND last_used < NOW() - INTERVAL 12 MONTH")
	int deleteClientsNotUsedIn12Months();
}
