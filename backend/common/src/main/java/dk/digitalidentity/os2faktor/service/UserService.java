package dk.digitalidentity.os2faktor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2faktor.dao.UserDao;
import dk.digitalidentity.os2faktor.dao.model.User;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {
	
	@Autowired
	private UserDao userDao;

	@Autowired
	private SsnService ssnService;
	
	public User getByPlainTextSsn(String ssn) {
		String encryptedSsn = null;

		try {
			encryptedSsn = ssnService.encryptAndEncodeSsn(ssn);
		}
		catch (Exception ex) {
			log.error("Failed to encrypt ssn", ex);
			return null;
		}
		
		return userDao.findBySsn(encryptedSsn);
	}
	
	public User getByEncodedSsn(String encodedSsn) {
		String encryptedSsn = null;

		try {
			encryptedSsn = ssnService.encryptAndEncodeEncodedSsn(encodedSsn);
		}
		catch (Exception ex) {
			log.warn("Failed to encrypt ssn", ex);
			return null;
		}
		
		return userDao.findBySsn(encryptedSsn);
	}
	
	public User getByEncryptedAndEncodedSsn(String encryptedAndEncodedSsn) {
		return userDao.findBySsn(encryptedAndEncodedSsn);
	}

	// not all users have a PID, so only use this as a last resort
	public User getByPid(String pid) {
		return userDao.findByPid(pid);
	}
	
	public User save(User user) throws Exception {
		// encrypt user SSN if needed (first time save)
		if (user.getSsn() != null && (user.getSsn().length() == 10 || user.getSsn().length() == 11)) {
			user.setSsn(ssnService.encryptAndEncodeSsn(user.getSsn()));
		}

		return userDao.save(user);
	}

	@Transactional
	public void deleteAncientUsers() {
		userDao.deleteUsersWithoutClients();
	}
}
