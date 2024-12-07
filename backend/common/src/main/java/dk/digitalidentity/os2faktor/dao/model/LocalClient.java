package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import javax.validation.constraints.NotNull;

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
	private String nsisLevel;

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
