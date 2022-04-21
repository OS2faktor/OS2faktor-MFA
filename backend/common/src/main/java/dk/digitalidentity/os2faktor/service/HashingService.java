package dk.digitalidentity.os2faktor.service;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HashingService {
	private IvParameterSpec iv;
	private SecretKey encryptionKey;

	public HashingService(@Value("${ssn.encryption.password}") String encryptionKeySeed) throws Exception {
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
	 * Performs a SHA-256 digest, then encrypts the value,
	 * and finally base64 encodes the value
	 * 
	 * @param string
	 * @return
	 */
	public String encryptAndEncodeString(String string) throws Exception {
		if (string == null) {
			return null;
		}

		// Digest
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] digest = md.digest(string.getBytes(Charset.forName("UTF-8")));

		// Encrypt
		byte[] encryptedDigestedSsn = encrypt(digest);

		// Base64 encode
		return Base64.getEncoder().encodeToString(encryptedDigestedSsn);
	}

	/**
	 * Checks if a string and an encoded string matches
	 * @param string SHA-256 digest, encrypted, base64 encoded and checked against encodedString
	 * @param encodedString checked against string
	 * @return returns false if any of the values are null or empty. Otherwise will return true if both strings match after encoding.
	 * @throws Exception throws exception if encryption went wrong
	 */
	public boolean matches(String string, String encodedString) throws Exception {
		String newEncodedString = encryptAndEncodeString(string);

		if (!StringUtils.hasLength(string) || !StringUtils.hasLength(encodedString)) {
			return false;
		}

		return Objects.equals(newEncodedString, encodedString);
	}

	private byte[] encrypt(byte[] data) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, iv);
		
		return cipher.doFinal(data);
	}
}
