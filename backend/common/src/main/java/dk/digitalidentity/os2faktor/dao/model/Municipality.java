package dk.digitalidentity.os2faktor.dao.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
