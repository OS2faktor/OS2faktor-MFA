package dk.digitalidentity.os2faktor.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "servers")
@Getter
@Setter
public class Server {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@NotNull
	@Size(min = 3, max = 255)
	private String name;
	
	@Column
	@NotNull
	@Size(min = 36, max = 36)
	private String apiKey;
	
	@Column
	private String connectorVersion;
	
	@ManyToOne
	@JoinColumn(name = "municipality_id")
	private Municipality municipality;
	
	@Column
	private long useCount;
	
	@Column
	private String tlsVersion;
}
