package dk.digitalidentity.os2faktor.service;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.os2faktor.service.model.ClientSearchParams;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LdapService {

	@Autowired
	private LdapTemplate ldapTemplate;
	
	@Value("${ldap.field.ssn}")
	private String ssnField;

	@Value("${ldap.field.pid}")
	private String pidField;

	@Value("${ldap.field.pseudonym}")
	private String pseudonymField;

	@Value("${ldap.field.deviceId}")
	private String deviceIdField;
	
	@Value("${ldap.access.group}")
	private String accessGroup;
	
	@Value("${ldap.usernameattribute:sAMAccountName}")
	private String usernameAttribute;

	public LdapService(@Value("${ldap.cert.trustall:false}") boolean trustAllCert) {
		if (trustAllCert) {
			allowUntrustedCert();
		}
	}

	public boolean verifyPassword(String username, String password) {
		EqualsFilter filter = new EqualsFilter(usernameAttribute, username);

		return ldapTemplate.authenticate("", filter.encode(), password);
	}
	
	public boolean isAllowedToUseRadius(String sAMAccountName) throws Exception {

		// if no configured group, allow all
		if (accessGroup == null || accessGroup.length() == 0) {
			return true;
		}

		List<DirContextOperations> result = ldapTemplate.search(query()
			.where("objectclass").is("person")
			.and(usernameAttribute).is(sAMAccountName), new AbstractContextMapper<DirContextOperations>() {

				@Override
				protected DirContextOperations doMapFromContext(DirContextOperations ctx) {
					return ctx;
				}
			}
		);

		if (result != null && result.size() == 1) {
			DirContextOperations personContext = result.get(0);
			Object[] attrs = personContext.getObjectAttributes("memberOf");

			if (attrs != null) {
				for (int i = 0; i < attrs.length; i++) {
					String foundMember = (String) attrs[i];
	
					if (foundMember.contains(accessGroup)) {
						return true;
					}
				}
			}

			return false;
		}
		
		log.error("Unexpected result from AD when looking up " + sAMAccountName + ". Got: " + ((result != null) ? result.size() : 0) + " results.");

		return false;
	}

	public ClientSearchParams getUserDetails(String sAMAccountName) throws Exception {
		List<ClientSearchParams> result = ldapTemplate.search(query().where("objectclass").is("user").and(usernameAttribute).is(sAMAccountName), new AbstractContextMapper<ClientSearchParams>() {

			@Override
			protected ClientSearchParams doMapFromContext(DirContextOperations ctx) {
				ClientSearchParams result = new ClientSearchParams();
				if (ssnField != null && !StringUtils.isEmpty(ssnField)) {
					result.setSsn((String) ctx.getObjectAttribute(ssnField));
				}
				if (pidField != null && !StringUtils.isEmpty(pidField)) {
					result.setPid((String) ctx.getObjectAttribute(pidField));
				}
				if (pseudonymField != null && !StringUtils.isEmpty(pseudonymField)) {
					result.setPseudonym((String) ctx.getObjectAttribute(pseudonymField));
				}
				if (deviceIdField != null && !StringUtils.isEmpty(deviceIdField)) {
					result.setDeviceId((String) ctx.getObjectAttribute(deviceIdField));
				}

				return result;
			}
		});
		
		// expect exactly one match
		if (result != null && result.size() == 1) {
			return result.get(0);
		}

		log.error("Unexpected result from AD when looking up " + sAMAccountName + ". Got: " + ((result != null) ? result.size() : 0) + " results.");
		
		return null;
	}
	
	private static void allowUntrustedCert() {
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					;
				}
				
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					;
				}
			}
		};

		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			SSLContext.setDefault(sc);
		}
		catch (Exception ex) {
			log.error("Failed to flag all certificates as trusted!", ex);
		}
	}
}
