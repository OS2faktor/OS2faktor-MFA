package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ExternalLoginSession {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column
	private Date tts;

	@Column
	private String ssn;

	@Column
	@Enumerated(EnumType.STRING)
	private NSISLevel nsisLevel;
	
	@ManyToOne
	@JoinColumn(name = "login_service_provider_id")
	private LoginServiceProvider loginServiceProvider;

	@Column
	private String sessionKey;
}
