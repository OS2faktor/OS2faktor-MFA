package dk.digitalidentity.os2faktor.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pseudonyms")
@Getter
@Setter
public class Pseudonym {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String cvr;
	
	@Column
	private String pseudonym;
	
	@Column
	private String ssn;
}
