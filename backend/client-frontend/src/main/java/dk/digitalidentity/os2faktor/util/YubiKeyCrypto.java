package dk.digitalidentity.os2faktor.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.yubico.webauthn.data.ByteArray;

public class YubiKeyCrypto {
    private static final Provider provider = new BouncyCastleProvider();

    public boolean verifySignature(PublicKey publicKey, ByteArray signedBytes, ByteArray signature) {
        try {
            Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA", provider);
            ecdsaSignature.initVerify(publicKey);
            ecdsaSignature.update(signedBytes.getBytes());

            return ecdsaSignature.verify(signature.getBytes());
        }
        catch (GeneralSecurityException ex) {
        	throw new RuntimeException(
                String.format(
                    "Failed to verify signature. This could be a problem with your JVM environment, or a bug in webauthn-server-core. Public key: %s, signed data: %s , signature: %s",
                    publicKey,
                    signedBytes.getBase64Url(),
                    signature.getBase64Url()
                ),
                ex
            );
        }
    }

    public ByteArray hash(ByteArray bytes) {
        try {
            return new ByteArray(MessageDigest.getInstance("SHA-256", provider).digest(bytes.getBytes()));
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
