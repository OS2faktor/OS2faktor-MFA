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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "partial_clients")
@Getter
@Setter
public class PartialClient {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@JsonIgnore
	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	@Column(updatable = false)
	private Date created;

	@Column(name = "client_type")
	@Enumerated(EnumType.STRING)
	private ClientType type;

	@Column
	@NotNull
	@Size(min = 2, max = 255)
	private String name;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	// for type authenticator_app this field is used for the secret
	@Column
	@NotNull
	private String challenge;
}
