package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

	@Id
	@Column
	private String subscriptionKey;

	@Column
	@NotNull
	private String pollingKey;

	@JsonIgnore
	@CreationTimestamp
	@Column(updatable = false)
	private Date created;

	@Column
	private Date sentTimestamp;

	@Column
	@NotNull
	private boolean clientNotified;
	
	@Column
	@NotNull
	private boolean clientAuthenticated;
	
	@Column
	@NotNull
	private boolean clientRejected;

	@Column
	private Date clientFetchedTimestamp;

	@Column
	private Date clientResponseTimestamp;

	@Column
	@NotNull
	private String challenge;

	@Column
	private String redirectUrl;

	@Column
	private boolean passwordless;
	
	@JsonIgnore
	@Column
	private String serverName;

	@JsonIgnore
	@Column
	private long serverId;
	
	@JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_device_id")
	private Client client;
	
	@Column
	private String clientType;
}
