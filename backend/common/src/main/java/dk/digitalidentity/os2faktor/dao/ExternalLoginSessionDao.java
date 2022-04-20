package dk.digitalidentity.os2faktor.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.ExternalLoginSession;

public interface ExternalLoginSessionDao extends JpaRepository<ExternalLoginSession, Long> {

	ExternalLoginSession findBySessionKey(String sessionKey);
}
