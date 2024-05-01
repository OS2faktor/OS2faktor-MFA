package dk.digitalidentity.os2faktor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2faktor.dao.LoginServiceProviderDao;
import dk.digitalidentity.os2faktor.dao.model.LoginServiceProvider;

@Service
public class LoginServiceProviderService {

	@Autowired
	private LoginServiceProviderDao loginServiceProviderDao;
	
	public LoginServiceProvider getByApiKey(String apiKey) {
		return loginServiceProviderDao.findByApiKey(apiKey);
	}
}
