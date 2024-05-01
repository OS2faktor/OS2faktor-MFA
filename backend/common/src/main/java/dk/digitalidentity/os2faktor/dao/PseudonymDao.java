package dk.digitalidentity.os2faktor.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.Pseudonym;

public interface PseudonymDao extends JpaRepository<Pseudonym, Long> {

	List<Pseudonym> findByCvr(String municipalityCVR);

	Pseudonym findByPseudonymAndCvr(String pseudonym, String cvr);

}
