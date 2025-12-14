package dk.digitalidentity.os2faktor.dao.model;

import java.time.LocalDateTime;

public record NotificationHistoryDTO(
	LocalDateTime created,
	LocalDateTime sentTimestamp,
	String serverName,
	boolean clientNotified,
	boolean clientAuthenticated,
	boolean clientRejected,
	String clientDeviceId,
	LocalDateTime clientFetchedTimestamp,
	LocalDateTime clientResponseTimestamp,
	String clientType,
	String systemName,
	String username
) {}
