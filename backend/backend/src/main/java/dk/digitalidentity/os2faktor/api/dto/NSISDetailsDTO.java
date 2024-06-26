package dk.digitalidentity.os2faktor.api.dto;

import java.util.Date;

import dk.digitalidentity.os2faktor.dao.model.Client;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NSISDetailsDTO {
	private String deviceId;
	private Date created;
	private Date associatedUserTimestamp;
	private String pid;
	private boolean locked;
	private Date lockedUntil;

	public NSISDetailsDTO(Client client) {
		this.deviceId = client.getDeviceId();
		this.created = client.getCreated();
		this.locked = client.isLocked();
		this.lockedUntil = client.getLockedUntil();
		this.associatedUserTimestamp = client.getAssociatedUserTimestamp();
		if (client.getUser() != null) {
			this.pid = client.getUser().getPid();
		}
	}
}
