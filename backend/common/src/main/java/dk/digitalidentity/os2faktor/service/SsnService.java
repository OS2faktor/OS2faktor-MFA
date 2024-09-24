package dk.digitalidentity.os2faktor.service;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SsnService {
	private IvParameterSpec iv;
	private SecretKey encryptionKey;

	public SsnService(@Value("${ssn.encryption.password}") String encryptionKeySeed) throws Exception {

		// generate password derived secret key
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(encryptionKeySeed.toCharArray(), new byte[] { 0x00, 0x01, 0x02, 0x03 }, 65536, 256);
		SecretKey tmp = factory.generateSecret(spec);
		encryptionKey = new SecretKeySpec(tmp.getEncoded(), "AES");
		
		// generate static IV
		byte[] ivData = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		iv = new IvParameterSpec(ivData);
	}

	/**
	 * Performs a base64 decode on the input, then encrypts the result,
	 * and finally base64 encode the encrypted value
	 * 
	 * @param ssn base64 encoded and sha256-digested ssn
	 * @return base64 encoded encrypted sha256-digested ssn
	 */
	public String encryptAndEncodeEncodedSsn(String ssn) throws Exception {
		return encryptAndEncodeEncodedSsn(ssn, true);
	}
		
	private String encryptAndEncodeEncodedSsn(String ssn, boolean firstTry) throws Exception {
		try {
			// base64 decode
			byte[] ssnDigest = Base64.getDecoder().decode(ssn);

			// encrypt
			byte[] encryptedDigestedSsn = encrypt(ssnDigest);
	
			// base64 encode
			return Base64.getEncoder().encodeToString(encryptedDigestedSsn);
		}
		catch (Exception ex) {
			if (firstTry && ssn.contains(" ")) {
				log.debug("Failed decoding ssn (try again with plus-conversion): " + ssn);

				// some clients send ssn with bad encoding (space where plus is needed)
				ssn = ssn.replace(' ', '+');

				return encryptAndEncodeEncodedSsn(ssn, false);
			}
			else {
				if (ssn != null && ssn.length() > 0 && ssn.endsWith("d")) {
					log.warn("Malformed encoded ssn: " + ssn);
				}
				else {
					log.error("Malformed encoded ssn: " + ssn);
				}
			}

			throw ex;
		}
	}

	/**
	 * Ensure input has not slashes (remove '-'), performs a SHA-256 digest,
	 * then encrypts the value, and finally base64 encodes the value
	 * 
	 * @param ssn
	 * @return
	 */
	public String encryptAndEncodeSsn(String ssn) throws Exception {

		// remove slashes
		ssn = ssn.replace("-", "");
		
		// digest
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] ssnDigest = md.digest(ssn.getBytes(Charset.forName("UTF-8")));

		// encrypt
		byte[] encryptedDigestedSsn = encrypt(ssnDigest);

		// base64 encode
		return Base64.getEncoder().encodeToString(encryptedDigestedSsn);
	}

	private byte[] encrypt(byte[] data) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, iv);
		
		return cipher.doFinal(data);
	}
}
