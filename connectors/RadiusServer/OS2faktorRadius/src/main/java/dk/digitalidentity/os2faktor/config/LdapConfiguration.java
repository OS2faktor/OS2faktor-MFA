package dk.digitalidentity.os2faktor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
public class LdapConfiguration {

	@Bean
	public ContextSource contextSource(
			@Value("${ldap.url}") String ldapUrl,
			@Value("${ldap.base}") String ldapBase,
			@Value("${ldap.username}") String ldapUsername,
			@Value("${ldap.password}") String ldapPassword) {

		LdapContextSource contextSource = new LdapContextSource();

		contextSource.setUrl(ldapUrl);
		contextSource.setBase(ldapBase);
		contextSource.setUserDn(ldapUsername);
		contextSource.setPassword(ldapPassword);

		return contextSource;
	}
	
	@Bean
	public LdapTemplate ldapTemplate(ContextSource contextSource) {
		LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
		ldapTemplate.setIgnorePartialResultException(true); // to make AD happy

		return ldapTemplate;
	}
}
