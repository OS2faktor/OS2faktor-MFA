package dk.digitalidentity.os2faktor.config;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.martijndwars.webpush.PushService;

@Configuration
public class PushServiceConfiguration {

	@Value("${vapid.public.key}")
	private String publicKey;
	
	@Value("${vapid.private.key}")
	private String privateKey;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Bean
	public PushService pushService() throws Exception {
	    return new PushService(publicKey, privateKey);
	}
}
