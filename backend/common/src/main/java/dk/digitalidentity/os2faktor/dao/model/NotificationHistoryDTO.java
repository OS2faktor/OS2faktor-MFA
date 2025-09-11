package dk.digitalidentity.os2faktor.dao.model;

import java.time.LocalDateTime;

public interface NotificationHistoryDTO {
    LocalDateTime getCreated();
    LocalDateTime getSentTimestamp();
    String getServerName();
    boolean isClientNotified();
    boolean isClientAuthenticated();
    boolean isClientRejected();
    String getClientDeviceId();
    LocalDateTime getClientFetchedTimestamp();
    LocalDateTime getClientResponseTimestamp();
    String getClientType();
}
