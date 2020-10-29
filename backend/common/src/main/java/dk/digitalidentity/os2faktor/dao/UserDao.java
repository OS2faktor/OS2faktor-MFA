package dk.digitalidentity.os2faktor.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.User;

public interface UserDao extends JpaRepository<User, Long> {
	User getBySsn(String ssn);

	User getByPid(String pid);
}
