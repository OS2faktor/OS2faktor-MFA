package dk.digitalidentity.os2faktor.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.LocalClient;

public interface LocalClientDao extends JpaRepository<LocalClient, Long> {
	LocalClient getByDeviceIdAndCvr(String deviceId, String cvr);
	List<LocalClient> getBySsnAndCvr(String ssn, String cvr);
	List<LocalClient> getByCvr(String cvr);
	
	void deleteByDeviceId(String deviceId);
	void deleteByDeviceIdAndCvr(String deviceId, String cvr);
}
