package dk.digitalidentity.os2faktor.service;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {
	private static SecureRandom random = new SecureRandom();

	@Autowired
	private ClientService clientService;

	public String generateUuid() {
		return UUID.randomUUID().toString();
	}
	
	public byte[] getRandomBytes(int size) {
		byte[] tmp = new byte[size];
		
		random.nextBytes(tmp);
		
		return tmp;
	}

    public String generateChallenge() {
        char[] legalChars = "QWERTYUOLPKJHGFDSAMNBVCXZ1234567890".toCharArray();

        StringBuilder builder = new StringBuilder();
        builder.append(legalChars[random.nextInt(legalChars.length)]);
        builder.append(legalChars[random.nextInt(legalChars.length)]);
        builder.append(legalChars[random.nextInt(legalChars.length)]);
        builder.append(legalChars[random.nextInt(legalChars.length)]);

        return builder.toString();
    }

	public String generateDeviceId() {
		String deviceId = null;

		while (true) {
			deviceId = generateRandomDeviceId();
	
			if (clientService.getByDeviceId(deviceId) == null) {
				break;
			}
		}
		
		return deviceId;
	}

	private String generateRandomDeviceId() {
		StringBuilder builder = new StringBuilder();
		
		int value = random.nextInt(1000);
		builder.append(valueToString(value));
		
		builder.append("-");
		
		value = random.nextInt(1000);
		builder.append(valueToString(value));

		builder.append("-");
		
		value = random.nextInt(1000);
		builder.append(valueToString(value));

		builder.append("-");
		
		value = random.nextInt(1000);
		builder.append(valueToString(value));

		return builder.toString();
	}

	private static String valueToString(int value) {
		if (value  < 10) {
			return "00" + value;
		}
		else if (value < 100) {
			return "0" + value;
		}
		
		return "" + value;
	}
}
