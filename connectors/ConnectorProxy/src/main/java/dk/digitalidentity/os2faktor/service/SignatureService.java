package dk.digitalidentity.os2faktor.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Enumeration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

@Service
public class SignatureService {
	private PrivateKey key;	
	private boolean enabled;

	public SignatureService(@Value("${login.enabled}") boolean enabled, @Value("${login.keystore.location}") String keystoreLocation, @Value("${login.keystore.password}") String password) throws Exception {
		this.enabled = enabled;

		if (!enabled) {
			return;
		}

		KeyStore keyStore = keyStore(keystoreLocation, password.toCharArray());
		Enumeration<String> aliases = keyStore.aliases();

		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			key = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
			break;
		}
	}

	public String sign(String data) throws Exception {
		if (!enabled) {
			throw new Exception("OS2faktor login is not enabled!");
		}

		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initSign(key);
		sig.update(data.getBytes(Charset.forName("UTF-8")));

		return Base64.getEncoder().encodeToString(sig.sign());
	}

	private KeyStore keyStore(String file, char[] password) throws Exception {
		if (!enabled) {
			throw new Exception("OS2faktor login is not enabled!");
		}

		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		File key = ResourceUtils.getFile(file);

		try (InputStream in = new FileInputStream(key)) {
			keyStore.load(in, password);
		}

		return keyStore;
	}
}
