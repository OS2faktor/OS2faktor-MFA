package dk.digitalidentity.os2faktor.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.ProjectionClient;

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

	/*
		SELECT STRAIGHT_JOIN
		  c.name, 
		  c.passwordless,
		  c.client_type, 
		  c.device_id, 
		  td.serialnumber, 
		  c.nsis_level, 
		  u.ssn, 
		  c.last_used,
		  c.associated_user_timestamp 
		FROM 
		  clients c 
		JOIN users u ON u.id = c.user_id 
		LEFT JOIN totph_devices td ON td.client_device_id = c.device_id 
		WHERE c.disabled = 0 
		ORDER BY c.device_id ASC
		LIMIT 5000
	 */
	@Query(nativeQuery = true, value = "SELECT STRAIGHT_JOIN c.name, c.passwordless, c.client_type, c.device_id, td.serialnumber, c.nsis_level, u.ssn, c.last_used, c.associated_user_timestamp FROM clients c JOIN users u ON u.id = c.user_id LEFT JOIN totph_devices td ON td.client_device_id = c.device_id WHERE c.disabled = 0 ORDER BY c.device_id ASC LIMIT 5000")
	List<ProjectionClient> getNonLocalClients();

	/*
		SELECT STRAIGHT_JOIN
		  c.name, 
		  c.passwordless,
		  c.client_type, 
		  c.device_id, 
		  td.serialnumber, 
		  c.nsis_level, 
		  u.ssn, 
		  c.last_used,
		  c.associated_user_timestamp 
		FROM 
		  clients c 
		JOIN users u ON u.id = c.user_id 
		LEFT JOIN totph_devices td ON td.client_device_id = c.device_id 
		WHERE c.device_id > ?1 AND c.disabled = 0 
		ORDER BY c.device_id ASC
		LIMIT 5000
	 */
	@Query(nativeQuery = true, value = "SELECT STRAIGHT_JOIN c.name, c.passwordless, c.client_type, c.device_id, td.serialnumber, c.nsis_level, u.ssn, c.last_used, c.associated_user_timestamp FROM clients c JOIN users u ON u.id = c.user_id LEFT JOIN totph_devices td ON td.client_device_id = c.device_id WHERE c.device_id > ?1 AND c.disabled = 0 ORDER BY c.device_id ASC LIMIT 5000")
	List<ProjectionClient> getNonLocalClients(String deviceId);
	
	/*
		SELECT 
		  c.name, 
		  c.passwordless,
		  c.client_type, 
		  c.device_id, 
		  td.serialnumber, 
		  lc.nsis_level, 
		  lc.ssn, 
		  c.last_used,
		  c.associated_user_timestamp 
		FROM 
		  local_clients lc FORCE INDEX (cvr_2)
		JOIN clients c ON c.device_id = lc.device_id 
		LEFT JOIN totph_devices td ON td.client_device_id = c.device_id 
		WHERE c.disabled = 0 AND lc.cvr = ?1
	 */
	@Query(nativeQuery = true, value = "SELECT c.name, c.passwordless, c.client_type, c.device_id, td.serialnumber, lc.nsis_level, lc.ssn, c.last_used,c.associated_user_timestamp FROM local_clients lc FORCE INDEX (cvr_2)JOIN clients c ON c.device_id = lc.device_id LEFT JOIN totph_devices td ON td.client_device_id = c.device_id WHERE c.disabled = 0 AND lc.cvr = ?1")
	List<ProjectionClient> getLocalClients(String cvr);
}
