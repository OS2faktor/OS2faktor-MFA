package dk.digitalidentity.os2faktor.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
