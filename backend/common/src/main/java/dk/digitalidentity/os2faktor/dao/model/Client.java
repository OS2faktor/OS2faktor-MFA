package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Getter
@Setter
public class Client {

	@Id
	@NotNull
	private String deviceId;

	@JsonIgnore
	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	@Column(updatable = false)
	private Date created;

	// the clients secret key, used for accepting/rejecting challenges
	@JsonIgnore
	@Column
	@NotNull
	@Size(min = 36, max = 36)
	private String apiKey;

	// used for controlling flow on backend
	@Column(name = "client_type")
	@Enumerated(EnumType.STRING)
	private ClientType type;

	// pretty name set by user, to be displayed in UI when picking MFA client
	@Column
	@NotNull
	@Size(min = 3, max = 255)
	private String name;

	// the key for contacting the client (usually a push-key, but could be phone-number, etc)
	@JsonIgnore
	@Column
	private String notificationKey;

	// not sure why we keep this, probably for cleanup purposes, it is the token
	// we use when registering a client at Amazon SNS.
	@JsonIgnore
	@Column
	private String token;

	@ManyToOne
	@JoinColumn(name = "user_id")
	@JsonIgnore
	private User user;
	
	@Column
	@JsonIgnore
	private String clientVersion;
	
	@Column
	@JsonIgnore
	private String pincode;
	
	private transient boolean hasPincode;

	@Column
	@JsonIgnore
	private long failedPinAttempts;

	@Column
	@JsonIgnore
	private boolean locked;

	@JsonIgnore
	@Temporal(TemporalType.TIMESTAMP)
	@Column
	private Date lockedUntil;
	
	// starts at 0 when registered, and goes up 1 each time accept/reject is hit
	@JsonIgnore
	@Column
	private long useCount;

	@JsonIgnore
	@UpdateTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	@Column
	private Date lastUsed;

	@Column
	@JsonIgnore
	private boolean disabled;
	
	@Column
	@JsonIgnore
	private String yubikeyUid;
	
	@Column
	@JsonIgnore
	private String yubikeyAttestation;
}
