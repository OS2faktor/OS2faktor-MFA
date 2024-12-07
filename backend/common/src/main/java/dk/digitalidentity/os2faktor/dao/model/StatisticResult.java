package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "statistics_result")
@Getter
@Setter
public class StatisticResult {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date tts;
	
	@Column
	private long logins;

	@Column
	private long serverId;
	
	@Column
	private String cvr;

	@Column
	private String municipalityName;

	@Column
	private String serverName;

}
