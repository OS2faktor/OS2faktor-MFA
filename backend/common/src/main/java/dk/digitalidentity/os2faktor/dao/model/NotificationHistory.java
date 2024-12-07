package dk.digitalidentity.os2faktor.dao.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistory {

	@Id
	@Column
	private String subscriptionKey;

	@Column
	private String pollingKey;

	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date created;

	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date sentTimestamp;

	@Column
	private boolean clientNotified;
	
	@Column
	private boolean clientAuthenticated;
	
	@Column
	private boolean clientRejected;

	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date clientFetchedTimestamp;

	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date clientResponseTimestamp;

	@Column
	private String challenge;

	@Column
	private String redirectUrl;

	@Column
	private String serverName;

	@Column
	private long serverId;
	
	@Column
	private String clientDeviceId;
}
