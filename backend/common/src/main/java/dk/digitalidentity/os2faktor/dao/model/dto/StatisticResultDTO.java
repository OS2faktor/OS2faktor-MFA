package dk.digitalidentity.os2faktor.dao.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatisticResultDTO {
	private String tts;
	private long logins;
	private String serverName;
}