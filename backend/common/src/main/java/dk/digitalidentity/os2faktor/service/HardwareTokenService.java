package dk.digitalidentity.os2faktor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2faktor.dao.HardwareTokenDao;
import dk.digitalidentity.os2faktor.dao.model.HardwareToken;

@Service
public class HardwareTokenService {

	@Autowired
	private HardwareTokenDao hardwareTokenDao;

	public HardwareToken getByClient(String deviceId) {
		return hardwareTokenDao.findByClientDeviceId(deviceId);
	}

	public HardwareToken getBySerialnumber(String serialnumber) {
		return hardwareTokenDao.findBySerialnumber(serialnumber);
	}

	public HardwareToken save(HardwareToken hardwareToken) {
		return hardwareTokenDao.save(hardwareToken);
	}
}
