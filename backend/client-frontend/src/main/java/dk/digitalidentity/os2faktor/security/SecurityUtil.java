package dk.digitalidentity.os2faktor.security;

import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import dk.digitalidentity.os2faktor.service.UserService;
import dk.digitalidentity.samlmodule.model.TokenUser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SecurityUtil {

	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private UserService userService;

	public boolean isAuthenticated() {
		if (SecurityContextHolder.getContext().getAuthentication() != null &&
				SecurityContextHolder.getContext().getAuthentication().getDetails() != null &&
				SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser) {
			return true;
		}

		return false;
	}

	public TokenUser getTokenUser() {
		if (!isAuthenticated()) {
			return null;
		}

		return (TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails();
	}

	@SneakyThrows
	public void updateTokenUser(TokenUser tokenUser) {
		
		// update/create user context on session
		String cpr = getCpr(tokenUser);
		if (cpr == null) {
			// TODO: throw some sort of error
		}

		// if the user does not exist, create the user
		User user = userService.getByPlainTextSsn(cpr);
		if (user == null) {
			// fallback to lookup by encoded cpr, as the PID lookup might have failed, and
			// we used our database for lookup
			user = userService.getByEncryptedAndEncodedSsn(cpr);
		}

        if (user == null) {
    		String pid = tokenUser.getUsername();

    		user = new User();
        	user.setClients(new ArrayList<Client>());		            
            user.setPid(pid);
            user.setSsn(cpr);

        	userService.save(user);
        }
        
        // store authenticated user on session
        HttpSession session = request.getSession();
        session.setAttribute(ClientSecurityFilter.SESSION_USER, user);
        session.setAttribute(ClientSecurityFilter.SESSION_ROLE, "ROLE_USER");

        // pull out the NSIS level from the token
		NSISLevel nsisLevel = NSISLevel.NONE;

		// try to pull an updated nsisLevel from token
		Map<String, Object> attributes = tokenUser.getAttributes();
		if (attributes.containsKey("https://data.gov.dk/concept/core/nsis/loa")) {
			Object loaObj = attributes.get("https://data.gov.dk/concept/core/nsis/loa");
			
			if (loaObj != null && loaObj instanceof String) {
				String loa = (String) loaObj;
				loa = loa.toUpperCase();
		
				try {
					nsisLevel = NSISLevel.valueOf(loa);
				}
				catch (Exception ex) {
					log.error("Failed to parse loa: " + loa);
				}
			}
			else {
				log.error("Undefined loa level on attribute: " + ((loaObj != null) ? loaObj.toString() : "<null>"));
			}
		}

		session.setAttribute(ClientSecurityFilter.SESSION_NSIS_LEVEL, nsisLevel.toString());
	}

	public String getCpr(TokenUser tokenUser) {
		Map<String, Object> attributes = tokenUser.getAttributes();
		if (attributes == null || attributes.isEmpty()) {
			return null;
		}

		Object cprObj = attributes.get("https://data.gov.dk/model/core/eid/cprNumber");
		if (!(cprObj instanceof String)) {
			return null;
		}

		return (String) cprObj;		
	}

	public String getCpr() {
		if (!isAuthenticated()) {
			return null;
		}

		TokenUser tokenUser = (TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails();

		return getCpr(tokenUser);
	}
}
