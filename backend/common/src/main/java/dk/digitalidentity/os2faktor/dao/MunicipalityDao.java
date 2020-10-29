package dk.digitalidentity.os2faktor.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.Municipality;

public interface MunicipalityDao extends JpaRepository<Municipality, Long> {

	Municipality getByApiKey(String apiKey);
	Municipality getByCvr(String cvr);

}
