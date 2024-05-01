package dk.digitalidentity.os2faktor.dao;

import dk.digitalidentity.os2faktor.dao.model.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;

public interface NotificationHistoryDao extends JpaRepository<NotificationHistory, String> {

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM notifications_history WHERE created < ?1 LIMIT 100000")
    void deleteByCreatedBefore(Date timestamp);
}
