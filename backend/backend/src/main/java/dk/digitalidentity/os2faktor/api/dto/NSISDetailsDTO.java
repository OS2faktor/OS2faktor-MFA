package dk.digitalidentity.os2faktor.api.dto;

import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class NSISDetailsDTO {
	private String deviceId;
	private Date created;
	private Date associatedUserTimestamp;
	private String pid;

	public NSISDetailsDTO(Client client) {
		this.deviceId = client.getDeviceId();
		this.created = client.getCreated();
		this.associatedUserTimestamp = client.getAssociatedUserTimestamp();
		if (client.getUser() != null) {
			this.pid = client.getUser().getPid();
		}
	}
}
