package dk.digitalidentity.os2faktor.dao.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "municipalities")
@Getter
@Setter
public class Municipality {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String cvr;

	@JsonIgnore
	@Column
	@NotNull
	@Size(min = 36, max = 36)
	private String apiKey;

	@Column
	private String name;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "municipality", orphanRemoval = true, cascade = CascadeType.ALL)
	private List<Server> servers;
}
