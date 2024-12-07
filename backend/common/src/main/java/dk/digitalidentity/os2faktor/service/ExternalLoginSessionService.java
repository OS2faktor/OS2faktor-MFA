package dk.digitalidentity.os2faktor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2faktor.dao.ExternalLoginSessionDao;
import dk.digitalidentity.os2faktor.dao.model.ExternalLoginSession;
import jakarta.transaction.Transactional;

@Service
public class ExternalLoginSessionService {
	
	@Autowired
	private ExternalLoginSessionDao externalLoginSessionDao;

	public ExternalLoginSession save(ExternalLoginSession entity) {
		return externalLoginSessionDao.save(entity);
	}

	public ExternalLoginSession findBySessionKey(String sessionKey) {
		return externalLoginSessionDao.findBySessionKey(sessionKey);
	}

	@Transactional
	public void deleteAll() {
		externalLoginSessionDao.deleteAll();
	}
}
