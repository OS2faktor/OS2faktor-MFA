package dk.digitalidentity.os2faktor.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.AttestationConveyancePreference;
import com.yubico.webauthn.data.RelyingPartyIdentity;

@Configuration
public class RelyingPartyConfig {

	@Autowired
	private CredentialRepository credentialRepository;

	@Value("${yubikey.domain}")
	private String domain;

	@Bean
	public RelyingParty relyingParty() {
		RelyingPartyIdentity identity = RelyingPartyIdentity.builder()
				.id(domain)
				.name("OS2faktor")
				.build();

		return RelyingParty.builder()
				.identity(identity)
				.credentialRepository(credentialRepository)
				.allowOriginPort(true)
				.attestationConveyancePreference(AttestationConveyancePreference.DIRECT)
				.build();
	}
}
