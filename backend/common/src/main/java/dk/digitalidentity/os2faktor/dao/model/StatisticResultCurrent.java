package dk.digitalidentity.os2faktor.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "statistics_result_current")
@Getter
@Setter
public class StatisticResultCurrent {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
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
