package dk.digitalidentity.os2faktor.service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialParameters;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

import dk.digitalidentity.os2faktor.dao.model.Notification;
import dk.digitalidentity.os2faktor.dao.model.PartialClient;

@Service
public class YubiKeyService {

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private RelyingParty relyingParty;

	// Yubikey Login
	public AssertionRequest startYubiKeyLogin(String deviceId) {
		StartAssertionOptions options = StartAssertionOptions.builder()
				.username(deviceId)
				.userVerification(UserVerificationRequirement.DISCOURAGED)
				.build();
		return relyingParty.startAssertion(options);
	}

	public AssertionResult finalizeYubiKeyLogin(Notification notification, String clientResponse) throws IOException, AssertionFailedException {
		PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> response = PublicKeyCredential.parseAssertionResponseJson(clientResponse);

		return relyingParty.finishAssertion(
				FinishAssertionOptions.builder()
						.request(getAssertionRequestFromNotification(notification))
						.response(response)
						.build()
		);
	}


	// Yubikey Registration
	public PublicKeyCredentialCreationOptions startYubiKeyRegistration(String name) {
		StartRegistrationOptions options = getStartRegistrationOptions(name);
		return relyingParty.startRegistration(options);
	}

	// using a record since we need to return not just the parsed client response but also the server created result object
	public record YubikeyRegistrationDTO(PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response, RegistrationResult result) { }
	public YubikeyRegistrationDTO finalizeYubikeyRegistration(PartialClient partialClient, String clientResponse) throws RegistrationFailedException, IOException {
		PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response = PublicKeyCredential.parseRegistrationResponseJson(clientResponse);

		FinishRegistrationOptions finishRegistrationOptions = FinishRegistrationOptions.builder()
				.request(getCreationOptionsFromPartialClient(partialClient))
				.response(response)
				.build();
		RegistrationResult registrationResult = relyingParty.finishRegistration(finishRegistrationOptions);

		return new YubikeyRegistrationDTO(response, registrationResult);
	}


	// Private methods
	private PublicKeyCredentialCreationOptions getCreationOptionsFromPartialClient(PartialClient partialClient) {
		return PublicKeyCredentialCreationOptions.builder()
				.rp(relyingParty.getIdentity())
				.user(getUserIdentity(partialClient.getName()))
				.challenge(ByteArray.fromBase64(partialClient.getChallenge()))
				.pubKeyCredParams(List.of(PublicKeyCredentialParameters.ES256, PublicKeyCredentialParameters.EdDSA, PublicKeyCredentialParameters.RS256))
				.build();
	}

	private AssertionRequest getAssertionRequestFromNotification(Notification notification) {
		// We cannot set the challenge after the fact so we generate another object, so we can use the default from the framework without hardcoding them
		AssertionRequest defaultStartingOptions = startYubiKeyLogin(notification.getClient().getDeviceId());

		return AssertionRequest.builder()
				.publicKeyCredentialRequestOptions(PublicKeyCredentialRequestOptions.builder()
						.challenge(ByteArray.fromBase64(notification.getChallenge()))
						.allowCredentials(defaultStartingOptions.getPublicKeyCredentialRequestOptions().getAllowCredentials())
						.rpId(defaultStartingOptions.getPublicKeyCredentialRequestOptions().getRpId())
						.userVerification(defaultStartingOptions.getPublicKeyCredentialRequestOptions().getUserVerification().orElse(UserVerificationRequirement.DISCOURAGED)) // Value set by us in start or default value
						.extensions(defaultStartingOptions.getPublicKeyCredentialRequestOptions().getExtensions())
						.build()
				)
				.username(defaultStartingOptions.getUsername())
				.build();
	}

	private UserIdentity getUserIdentity(String name) {
		return UserIdentity.builder()
				.name(name)
				.displayName(name)
				.id(ByteArray.fromBase64(Base64.getEncoder().encodeToString(idGenerator.getRandomBytes(16))))
				.build();
	}

	private AuthenticatorSelectionCriteria getAuthenticatorSelectionCriteria() {
		return AuthenticatorSelectionCriteria.builder()
				.userVerification(UserVerificationRequirement.DISCOURAGED)
				// allow both
				//.authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
				.build();
	}

	private StartRegistrationOptions getStartRegistrationOptions(String name) {
		UserIdentity userIdentity = getUserIdentity(name);

		AuthenticatorSelectionCriteria selectionCriteria = getAuthenticatorSelectionCriteria();

		return StartRegistrationOptions.builder()
				.user(userIdentity)
				.authenticatorSelection(selectionCriteria)
				.build();
	}
}
