package dk.digitalidentity.os2faktor.dao.model;

import java.time.LocalDateTime;

public interface ProjectionClient {
	String getName();
	boolean isPasswordless();
	String getClientType();
	String getDeviceId();
	String getSerialnumber();
	String getNsisLevel();
	String getSsn();
	LocalDateTime getLastUsed();
	LocalDateTime getAssociatedUserTimestamp();
}
