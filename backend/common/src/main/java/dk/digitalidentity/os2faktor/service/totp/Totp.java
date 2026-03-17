package dk.digitalidentity.os2faktor.service.totp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Totp {
    private final String secret;
    private final Clock clock;
    private static final int DELAY_WINDOW = 1;
    private final Hash hashAlgo;

    public Totp(String secret) {
        this.secret = secret;
        clock = new Clock();
        this.hashAlgo = Hash.SHA1;
    }

    public Totp(String secret, Clock clock) {
        this.secret = secret;
        this.clock = clock;
        this.hashAlgo = Hash.SHA1;
    }

    public Totp(String secret, Clock clock, Hash hashAlgo) {
        this.secret = secret;
        this.clock = clock;
        this.hashAlgo = hashAlgo;
    }

    public String uri(String name) {
        try {
            return String.format("otpauth://totp/%s?secret=%s", URLEncoder.encode(name, "UTF-8"), secret);
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public boolean verify(String otp) {
        long code = Long.parseLong(otp);
        long currentInterval = clock.getCurrentInterval();

        int pastResponse = Math.max(DELAY_WINDOW, 0);

        for (int i = pastResponse; i >= 0; --i) {
            int candidate = generate(this.secret, currentInterval - i);
            if (candidate == code) {
                return true;
            }
        }
        
        return false;
    }

    private int generate(String secret, long interval) {
        return hash(secret, interval);
    }

    private int hash(String secret, long interval) {
        byte[] hash = new byte[0];

        try {
            hash = new Hmac(this.hashAlgo, Base32.decode(secret), interval).digest();
        }
        catch (Exception ex) {
        	log.error("Failed to generate Hmac", ex);
        }

        return bytesToInt(hash);
    }

    private int bytesToInt(byte[] hash) {
        int offset = hash[hash.length - 1] & 0xf;

        int binary = ((hash[offset] & 0x7f) << 24) |
                ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) << 8) |
                (hash[offset + 3] & 0xff);

        return binary % 1000000;
    }
}
