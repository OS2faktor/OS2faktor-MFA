package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Getter
@Setter
@BatchSize(size = 50)
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
	private String apiKey;

	// used for controlling flow on backend
	@Column(name = "client_type")
	@Enumerated(EnumType.STRING)
	private ClientType type;

	// the registered nsisLevel on this client (might be overruled by the localClient registration)
	@Column
	@Enumerated(EnumType.STRING)
	private NSISLevel nsisLevel;

	// pretty name set by user, to be displayed in UI when picking MFA client
	@Column
	@NotNull
	@Size(min = 2, max = 255)
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

	// unique id generated per device/app used to prevent duplicating/cloning clients
	@JsonIgnore
	@Column
	private String uniqueClientId;

	@ManyToOne
	@JoinColumn(name = "user_id")
	@JsonIgnore
	private User user;

	@JsonIgnore
	@Temporal(TemporalType.TIMESTAMP)
	private Date associatedUserTimestamp;
	
	@Column
	@JsonIgnore
	private String clientVersion;
	
	@Column
	@JsonIgnore
	private String pincode;
	
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

    @JsonFormat(pattern = "yyyy-MM-dd")
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

	@Column
	private boolean prime;
	
	@JsonIgnore
	@Column
	private boolean roaming;

	private transient boolean hasPincode;
	
	@Column
	@JsonIgnore
	private String secret;
	
	@Column(name = "robot_mfa")
	private boolean robotMFA;
}
