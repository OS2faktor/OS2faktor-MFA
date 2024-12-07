package dk.digitalidentity.os2faktor.dao.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String ssn;

	@Column
	private String pid;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "user", orphanRemoval = true, cascade = CascadeType.ALL)
	private List<Client> clients;
}
