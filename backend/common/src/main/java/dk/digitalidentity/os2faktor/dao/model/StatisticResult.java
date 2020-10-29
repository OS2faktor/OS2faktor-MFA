package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
