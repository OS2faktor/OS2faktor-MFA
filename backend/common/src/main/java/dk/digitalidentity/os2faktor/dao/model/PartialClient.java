package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
