package dk.digitalidentity.os2faktor.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.LoginServiceProvider;

public interface LoginServiceProviderDao extends JpaRepository<LoginServiceProvider, Long> {

	LoginServiceProvider getByApiKey(String apiKey);

}
