package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
