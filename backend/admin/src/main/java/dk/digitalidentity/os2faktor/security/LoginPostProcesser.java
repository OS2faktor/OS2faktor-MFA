package dk.digitalidentity.os2faktor.security;

import dk.digitalidentity.os2faktor.dao.MunicipalityDao;
import dk.digitalidentity.os2faktor.dao.model.Municipality;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class LoginPostProcesser implements SamlLoginPostProcessor {

	@Autowired
	private MunicipalityDao municipalityDao;

	@Override
	@Transactional
	public void process(TokenUser tokenUser) {
		String uuid = (String) tokenUser.getAttributes().get(TokenUser.ATTRIBUTE_UUID);
		String name = (String) tokenUser.getAttributes().get(TokenUser.ATTRIBUTE_NAME);
		String cvr = tokenUser.getCvr();

		Municipality municipality = municipalityDao.findByCvr(cvr);
		
		if (municipality == null) {
			log.warn("Rejected access to user: " + name + "/" + uuid + "/" + cvr + " because not signed up");
			throw new DisabledException("Din kommune er ikke tilsluttet OS2faktor. For at få adgang til denne portal, skal du kontakte OS2.");
		}
		
		boolean isAdmin = tokenUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_http://os2faktor.dk/roles/usersystemrole/admin/1"));		
		if (!isAdmin) {
			log.warn("Rejected access to user: " + name + "/" + uuid + "/" + cvr + " because no admin role");
			throw new InsufficientAuthenticationException("Du har ikke rettigheder til at tilgå administrator portalen");
		}

		AuthenticatedUser user = new AuthenticatedUser();
		user.setCvr(cvr);
		user.setName(name);
		user.setUuid(uuid);
		tokenUser.getAttributes().put("user", user);

		List<SamlGrantedAuthority> newAuthorities = new ArrayList<>();
		newAuthorities.add(new SamlGrantedAuthority(AuthenticatedUser.ROLE_ADMIN));
		tokenUser.setAuthorities(newAuthorities);
	}
}
