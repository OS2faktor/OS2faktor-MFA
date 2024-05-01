package dk.digitalidentity.os2faktor.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.LocalClient;

public interface LocalClientDao extends JpaRepository<LocalClient, Long> {
	LocalClient findByDeviceIdAndCvr(String deviceId, String cvr);
	List<LocalClient> findBySsnAndCvr(String ssn, String cvr);
	List<LocalClient> findByCvr(String cvr);
	
	void deleteByDeviceId(String deviceId);
	void deleteByDeviceIdAndCvr(String deviceId, String cvr);
}
