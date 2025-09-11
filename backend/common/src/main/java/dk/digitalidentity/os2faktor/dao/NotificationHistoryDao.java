package dk.digitalidentity.os2faktor.dao;

import dk.digitalidentity.os2faktor.dao.model.NotificationHistory;
import dk.digitalidentity.os2faktor.dao.model.NotificationHistoryDTO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface NotificationHistoryDao extends JpaRepository<NotificationHistory, String> {

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM notifications_history WHERE created < ?1 LIMIT 100000")
    void deleteByCreatedBefore(Date timestamp);

	@Query(nativeQuery = true, value = """
       SELECT created, sent_timestamp, server_name, client_notified, client_authenticated, client_rejected, client_device_id, client_fetched_timestamp, client_response_timestamp, client_type
       FROM notifications_history WHERE created >= ?2
       AND server_id IN (SELECT id FROM servers WHERE municipality_id = (SELECT id FROM municipalities WHERE cvr = ?1));
	""")
	List<NotificationHistoryDTO> getHistory(String cvr, LocalDateTime afterTts);
}
