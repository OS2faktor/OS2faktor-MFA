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
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "local_clients")
@Getter
@Setter
public class LocalClient {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private String cvr;
	
	@Column
	private String deviceId;

	@Column
	private String ssn;

	@Column
	@NotNull
	private String adminUserName;

	@Column
	@NotNull
	private String adminUserUuid;

	@Temporal(TemporalType.TIMESTAMP)
	@Column
	private Date ts;
}
