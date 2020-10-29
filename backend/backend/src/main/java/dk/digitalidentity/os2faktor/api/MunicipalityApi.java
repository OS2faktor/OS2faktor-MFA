package dk.digitalidentity.os2faktor.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.api.model.PseudonymDTO;
import dk.digitalidentity.os2faktor.dao.PseudonymDao;
import dk.digitalidentity.os2faktor.dao.model.Municipality;
import dk.digitalidentity.os2faktor.dao.model.Pseudonym;
import dk.digitalidentity.os2faktor.security.AuthorizedMunicipalityHolder;
import dk.digitalidentity.os2faktor.service.SsnService;

@CrossOrigin
@RestController
public class MunicipalityApi {

	@Autowired
	private PseudonymDao pseudonymDao;

	@Autowired
	private SsnService ssnService;

	@PostMapping("/api/municipality/pseudonyms")
	public ResponseEntity<?> loadPseudonyms(@RequestBody List<PseudonymDTO> body) throws Exception {
		Municipality municipality = AuthorizedMunicipalityHolder.getMunicipality();
		List<Pseudonym> newPseudonyms = new ArrayList<>();
		List<Pseudonym> oldPseudonyms = pseudonymDao.getByCvr(municipality.getCvr());

		for (PseudonymDTO pseudonym : body) {
			String encodedSsn = pseudonym.getSsn();
			String encodedAndEncryptedSsn = ssnService.encryptAndEncodeEncodedSsn(encodedSsn);

			Pseudonym p = new Pseudonym();
			p.setCvr(municipality.getCvr());
			p.setPseudonym(pseudonym.getPseudonym());
			p.setSsn(encodedAndEncryptedSsn);
			
			boolean found = false;
			for (Pseudonym p2 : newPseudonyms) {
				if (p2.getPseudonym().equals(p.getPseudonym())) {
					found = true;
					break;
				}
			}

			if (!found) {
				newPseudonyms.add(p);
			}
		}

		// Bulk save
		// TODO: find a way to perform updates rather than delete/insert
		pseudonymDao.deleteAll(oldPseudonyms);
		pseudonymDao.saveAll(newPseudonyms);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
