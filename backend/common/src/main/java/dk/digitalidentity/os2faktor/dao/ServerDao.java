package dk.digitalidentity.os2faktor.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.Server;

public interface ServerDao extends JpaRepository<Server, Long> {
	Server getByApiKey(String apiKey);
}
