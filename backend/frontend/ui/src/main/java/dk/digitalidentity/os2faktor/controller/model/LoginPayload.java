package dk.digitalidentity.os2faktor.controller.model;

import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.data.AuthenticatorData;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.CollectedClientData;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginPayload {

	@JsonIgnore
	private ObjectMapper mapper = new ObjectMapper();

	private String id;
	private String clientDataJson;
	private String authenticatorData;
	private String signature;
	private String userHandle;
	
	public CollectedClientData decodeClientDataJson() throws Exception {
		return new CollectedClientData(clientDataJsonAsByteArray());
	}
	
	public AuthenticatorData decodeAuthenticatorData() throws Exception {
        return new AuthenticatorData(authenticatorDataAsByteArray());
	}
	
	public ByteArray clientDataJsonAsByteArray() {
		byte[] raw = Base64.getDecoder().decode(clientDataJson);
		
		return new ByteArray(raw);
	}
	
	public ByteArray authenticatorDataAsByteArray() {
		byte[] raw = Base64.getDecoder().decode(authenticatorData);
		
        return new ByteArray(raw);
	}
	
	public ByteArray signatureAsByteArray() {
		byte[] raw = Base64.getDecoder().decode(signature);
		
        return new ByteArray(raw);
	}
}
