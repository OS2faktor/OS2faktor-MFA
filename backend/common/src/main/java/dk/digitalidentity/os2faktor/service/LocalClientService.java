package dk.digitalidentity.os2faktor.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2faktor.dao.LocalClientDao;
import dk.digitalidentity.os2faktor.dao.model.LocalClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LocalClientService {
	
	@Autowired
	private LocalClientDao localClientDao;

	@Autowired
	private SsnService ssnService;
	
	public List<LocalClient> getByEncodedSsnAndCvr(String encodedSsn, String cvr) {
		String encryptedSsn = null;
		
		try {
			encryptedSsn = ssnService.encryptAndEncodeEncodedSsn(encodedSsn);
		}
		catch (Exception ex) {
			log.error("Failed to encrypt ssn for cvr: " + cvr, ex);
			return null;
		}
		
		return localClientDao.findBySsnAndCvr(encryptedSsn, cvr);
	}
	
	public List<LocalClient> getByCvr(String cvr) {
		return localClientDao.findByCvr(cvr);
	}
	
	public LocalClient getByDeviceIdAndCvr(String deviceId, String cvr) {
		return localClientDao.findByDeviceIdAndCvr(deviceId, cvr);
	}
	
	public LocalClient save(LocalClient localClient) {
		return localClientDao.save(localClient);
	}
	
	@Transactional
	public void deleteByDeviceId(String deviceId) {
		localClientDao.deleteByDeviceId(deviceId);
	}
}
