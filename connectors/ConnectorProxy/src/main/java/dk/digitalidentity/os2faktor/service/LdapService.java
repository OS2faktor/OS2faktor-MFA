package dk.digitalidentity.os2faktor.service;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Name;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.os2faktor.service.model.ClientSearchParams;
import dk.digitalidentity.os2faktor.service.model.UsernameAndPassword;
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

	@Value("${ldap.password.change.prevent.group}")
	private String passwordChangePreventGroup;

	@Value("${login.enabled}")
	private boolean loginEnabled;
	
	@Value("${password.reset.enabled}")
	private boolean passwordResetEnabled;
	
	public LdapService(@Value("${ldap.cert.trustall:false}") boolean trustAllCert) {
		if (trustAllCert) {
			allowUntrustedCert();
		}
	}

	public ClientSearchParams getUserDetails(String sAMAccountName) throws Exception {
		if (!loginEnabled) {
			throw new Exception("OS2faktor login is not enabled");
		}

		List<ClientSearchParams> result = ldapTemplate.search(query().where("objectclass").is("user").and("sAMAccountName").is(sAMAccountName), new AbstractContextMapper<ClientSearchParams>() {

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
	
	public List<String> getSAMAccountNames(String ssn) throws Exception {
		if (!passwordResetEnabled) {
			throw new Exception("Password reset is not enabled");
		}

		if (ssn == null || ssn.length() != 10) {
			log.warn("Cannot lookup sAMAccountNames with ssn = '" + ssn + "' - it needs to be 10 characters in length");
			return new ArrayList<>();
		}

		// search with the given SSN (which does not have a dash)
		List<DirContextOperations> result1 = ldapTemplate.search(query()
			.where("objectclass").is("person")
			.and(ssnField).is(ssn), new AbstractContextMapper<DirContextOperations>() {

				@Override
				protected DirContextOperations doMapFromContext(DirContextOperations ctx) {
					return ctx;
				}
			}
		);

		// now search for all with a dash added (because...)
		String ssnWithSlash = ssn.substring(0, 6) + "-" + ssn.substring(6);		
		List<DirContextOperations> result2 = ldapTemplate.search(query()
			.where("objectclass").is("person")
			.and(ssnField).is(ssnWithSlash), new AbstractContextMapper<DirContextOperations>() {

				@Override
				protected DirContextOperations doMapFromContext(DirContextOperations ctx) {
					return ctx;
				}
			}
		);
		
		List<DirContextOperations> result = new ArrayList<>();
		if (result1 != null) {
			result.addAll(result1);
		}
		if (result2 != null) {
			result.addAll(result2);
		}

		List<String> sAMAccountNames = new ArrayList<>();
		for (int i = 0; i < result.size(); i++) {
			DirContextOperations personContext = result.get(i);
			String sAMAccountName = (String) personContext.getObjectAttribute("sAMAccountName");

			sAMAccountNames.add(sAMAccountName);
		}
		
		return sAMAccountNames;
	}

	
	public boolean isAllowedToChangePassword(String sAMAccountName) throws Exception {
		if (!passwordResetEnabled) {
			throw new Exception("Password reset is not enabled");
		}

		DirContextOperations personContext = getDirContextOperations(sAMAccountName);
		Object[] attrs = personContext.getObjectAttributes("memberOf");

		if (attrs != null) {
			for (int i = 0; i < attrs.length; i++) {
				String foundMember = (String) attrs[i];

				if (foundMember.contains(passwordChangePreventGroup)) {
					return false;
				}
			}
		}

		return true;
	}

	public UsernameAndPassword resetPassword(String sAMAccountName, String newPassword) throws Exception {
		if (!passwordResetEnabled) {
			throw new Exception("Password reset is not enabled");
		}

		DirContextOperations ctx = getDirContextOperations(sAMAccountName);
		String upn = (String) ctx.getObjectAttribute("userPrincipalName");
		Name dn = ctx.getDn();

		final String password = "\"" + newPassword + "\"";
	    ModificationItem changePwd = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("unicodepwd", password.getBytes("UTF-16LE")));

	    List<ModificationItem> mods = new ArrayList<>();
	    mods.add(changePwd);

    	ldapTemplate.modifyAttributes(dn, mods.toArray(new ModificationItem[0]));
		
    	UsernameAndPassword usernameAndPassword = new UsernameAndPassword();
    	usernameAndPassword.setPassword(password.replace("\"", ""));
    	usernameAndPassword.setUsername(upn);
    	
		return usernameAndPassword;
	}

	public String maskSsn(String ssn) {
		if (ssn != null && ssn.length() == 10) {
			return ssn.substring(0, 6) + "-xxxx";
		}
		
		// something is wrong with the ssn anyway
		return ssn;
	}

	private DirContextOperations getDirContextOperations(String sAMAccountName) throws Exception {
		List<DirContextOperations> result = ldapTemplate.search(query()
			.where("objectclass").is("person")
			.and("sAMAccountName").is(sAMAccountName), new AbstractContextMapper<DirContextOperations>() {

				@Override
				protected DirContextOperations doMapFromContext(DirContextOperations ctx) {
					return ctx;
				}
			}
		);

		// should NEVER happen
		if (result == null || result.size() == 0 || result.size() > 1) {
			throw new Exception("Found " + ((result != null) ? result.size() : -1) + " users with sAMAccountName " + sAMAccountName);
		}

		return result.get(0);
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
