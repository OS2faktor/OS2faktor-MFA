package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
	@Temporal(TemporalType.TIMESTAMP)
	@Column(updatable = false)
	private Date created;

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
	@NotNull
	private String challenge;

	@Column
	private String redirectUrl;

	@JsonIgnore
	@Column
	private String serverName;

	@JsonIgnore
	@Column
	private long serverId;
	
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "client_device_id")
	private Client client;
}
