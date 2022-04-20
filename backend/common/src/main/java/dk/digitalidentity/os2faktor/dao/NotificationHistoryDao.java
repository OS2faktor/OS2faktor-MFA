package dk.digitalidentity.os2faktor.dao;

import dk.digitalidentity.os2faktor.dao.model.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;

public interface NotificationHistoryDao extends JpaRepository<NotificationHistory, String> {
    void deleteByCreatedBefore(Date timestamp);
}
