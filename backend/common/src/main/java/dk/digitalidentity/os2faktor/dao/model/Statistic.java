package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "statistics")
@Getter
@Setter
public class Statistic {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private long serverId;
	
	@Column
	private String cvr;
	
	@Column
	private String deviceId;
	
	@Column
	private String clientType;
	
	@Column
	private String clientVersion;
	
	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	@Column
	private Date created;
}
