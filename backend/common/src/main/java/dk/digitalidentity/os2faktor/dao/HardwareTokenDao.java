package dk.digitalidentity.os2faktor.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.HardwareToken;

public interface HardwareTokenDao extends JpaRepository<HardwareToken, Long> {
	HardwareToken findBySerialnumber(String serialnumber);
	HardwareToken findByClientDeviceId(String deviceId);
}
