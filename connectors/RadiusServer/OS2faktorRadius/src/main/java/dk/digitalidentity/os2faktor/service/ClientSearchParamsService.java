package dk.digitalidentity.os2faktor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2faktor.service.model.ClientSearchParams;

@Service
public class ClientSearchParamsService {

	@Autowired
	private LdapService ldapService;
	
	@Autowired
	private IdentifyService identifyService;
	
	@Autowired
	private OS2faktorCprLookupService os2faktorService;
	
	@Value("${cpr.source:LDAP}")
	private String cprSource;
	
	public ClientSearchParams getUserDetails(String sAMAccountName) throws Exception {
		if ("IDENTIFY".equals(cprSource)) {
			return identifyService.getUserDetails(sAMAccountName);
		}
		else if ("OS2FAKTOR".equals(cprSource)) {
			return os2faktorService.getUserDetails(sAMAccountName);
		}
		else {
			return ldapService.getUserDetails(sAMAccountName);
		}
	}
}
