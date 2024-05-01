package dk.digitalidentity.os2faktor.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dk.digitalidentity.os2faktor.dao.model.User;

public interface UserDao extends JpaRepository<User, Long> {
	User findBySsn(String ssn);

	User findByPid(String pid);

	@Modifying
	@Query(nativeQuery = true, value = "DELETE u FROM os2faktor.users u LEFT JOIN clients c ON c.user_id = u.id WHERE c.device_id IS NULL")
	void deleteUsersWithoutClients();
}
