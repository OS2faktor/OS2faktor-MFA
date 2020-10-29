package dk.digitalidentity.os2faktor.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.PartialClient;

public interface PartialClientDao extends JpaRepository<PartialClient, Long> {

	PartialClient getById(long id);
}
