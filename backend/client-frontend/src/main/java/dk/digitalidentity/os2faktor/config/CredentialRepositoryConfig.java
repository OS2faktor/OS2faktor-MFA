package dk.digitalidentity.os2faktor.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AttestationObject;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;

import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.service.ClientService;
import lombok.extern.slf4j.Slf4j;

/**
 * We use this configuration to hook our concept of users into the yubico webauthn framework
 */
@Slf4j
@Component
public class CredentialRepositoryConfig implements CredentialRepository {

	@Autowired
	private ClientService clientService;

	// Get the credential IDs of all credentials registered to the user with the given username.
	@Override
	public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
		Client client = clientService.getByDeviceId(username);

		if (client == null) {
			return Set.of();
		}
		else {
			return Set.of(PublicKeyCredentialDescriptor.builder()
					.id(ByteArray.fromBase64(client.getYubikeyUid()))
					.type(PublicKeyCredentialType.PUBLIC_KEY)
					.build());
		}
	}

	// Get the user handle corresponding to the given username - the inverse of getUsernameForUserHandle(ByteArray).
	@Override
	public Optional<ByteArray> getUserHandleForUsername(String username) {
		if (StringUtils.hasLength(username)) {
			return Optional.of(new ByteArray(username.getBytes(StandardCharsets.UTF_8)));
		}
		return Optional.empty();
	}

	// Get the username corresponding to the given user handle - the inverse of getUserHandleForUsername(String).
	@Override
	public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
		if (!userHandle.isEmpty()) {
			return Optional.of(new String(userHandle.getBytes(), StandardCharsets.UTF_8));
		}

		return Optional.empty();
	}

	// Look up the public key and stored signature count for the given credential registered to the given user.
	@Override
	public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
		Client client = clientService.getByYubikeyUid(credentialId.getBase64());

		if (client == null) {
			return Optional.empty();
		}

		try {
			byte[] attestationBytes = Base64.getDecoder().decode(client.getYubikeyAttestation());
			AttestationObject attestationObject = new AttestationObject(new ByteArray(attestationBytes));
			ByteArray publicKeyBytes = attestationObject.getAuthenticatorData().getAttestedCredentialData().orElseThrow().getCredentialPublicKey();

			return Optional.of(RegisteredCredential.builder()
					.credentialId(credentialId)
					.userHandle(userHandle)
					.publicKeyCose(publicKeyBytes)
					.build());
		}
		catch (IOException ex) {
			log.error("Could not parse attestation object on client id: + '" + client.getDeviceId() + "'", ex);
			return Optional.empty();
		}
	}

	// Look up all credentials with the given credential ID, regardless of what user they're registered to.
	@Override
	public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
		Client client = clientService.getByYubikeyUid(credentialId.getBase64());

		if (client == null) {
			return Set.of();
		}

		try {
			byte[] attestationBytes = Base64.getDecoder().decode(client.getYubikeyAttestation());
			AttestationObject attestationObject = new AttestationObject(new ByteArray(attestationBytes));
			ByteArray publicKeyBytes = attestationObject.getAuthenticatorData().getAttestedCredentialData().orElseThrow().getCredentialPublicKey();

			return Set.of(RegisteredCredential.builder()
					.credentialId(credentialId)
					.userHandle(getUserHandleForUsername(client.getDeviceId()).orElseThrow())
					.publicKeyCose(publicKeyBytes)
					.build());
		}
		catch (IOException ex) {
			log.error("Could not parse attestation object on client id: + '" + client.getDeviceId() + "'", ex);
			return Set.of();
		}
	}
}
