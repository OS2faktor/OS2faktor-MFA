package dk.digitalidentity.os2faktor.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
