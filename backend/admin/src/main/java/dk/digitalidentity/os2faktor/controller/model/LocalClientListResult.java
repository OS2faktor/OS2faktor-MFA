package dk.digitalidentity.os2faktor.controller.model;

import java.util.Date;

import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.LocalClient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocalClientListResult {
	private String deviceId;
	private String name;
	private String adminUserName;
	private Date ts;

	public LocalClientListResult(LocalClient localClient, Client client) {
		this.deviceId = localClient.getDeviceId();
		this.adminUserName = localClient.getAdminUserName();
		this.ts = localClient.getTs();
		this.name = client != null ?  client.getName() : "";
	}

}
