package dk.digitalidentity.os2faktor.api.dto;

import java.util.List;

import dk.digitalidentity.os2faktor.dao.model.ProjectionClient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginMfaResponse {
	private String lastClientDeviceId;
	private long count;
	private List<ProjectionClient> clients;
}
