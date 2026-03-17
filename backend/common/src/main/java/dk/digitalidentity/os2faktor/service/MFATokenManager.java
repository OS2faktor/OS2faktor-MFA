package dk.digitalidentity.os2faktor.service;

import java.io.ByteArrayOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import dk.digitalidentity.os2faktor.service.totp.Base32;
import dk.digitalidentity.os2faktor.service.totp.Hash;
import dk.digitalidentity.os2faktor.service.totp.IncrementalClock;
import dk.digitalidentity.os2faktor.service.totp.Totp;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MFATokenManager {

	public String generateSecretKey() {
		return Base32.random();
	}

	public String getQRCode(String secret, String name) {
		String contents = "otpauth://totp/OS2faktor" + ":" + name + "?secret=" + secret + "&issuer=" + "OS2faktor" + "&algorithm=SHA1" + "&digits=6" + "&period=30";

		byte[] qrPng = null;
		QRCodeWriter writer = new QRCodeWriter();
		try {
			BitMatrix bitMatrix = writer.encode(contents, BarcodeFormat.QR_CODE, 320, 320);
			ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

			qrPng = pngOutputStream.toByteArray();
		}
		catch (Exception e) {
			log.warn("Failed to generate QR code for new (partial) client with name: " + name);
			return null;
		}

		if (qrPng == null) {
			return null;
		}

		return getDataUriForImage(qrPng);
	}

	private String getDataUriForImage(byte[] data) {
		Base64 base64Codec = new Base64();
		String encodedData = new String(base64Codec.encode(data));

		return String.format("data:%s;base64,%s", "image/png", encodedData);
	}

	public record OtpVerificationResult (boolean success, int offsetResult) { }
	public OtpVerificationResult verifyTotp(String code, String secret, int initialOffset, int span, Hash hashAlgo) {
		code = code.replace(" ", "");
		if (!isValidLong(code)) {
			return new OtpVerificationResult(false, 0);
		}
		
		// validate with +/- 90 seconds
		IncrementalClock[] clocks = getClocks(initialOffset, span);
		for (int i = 0; i < clocks.length; i++) {
			Totp totp = new Totp(secret, clocks[i], hashAlgo);
			boolean valid = totp.verify(code);

			if (valid) {
				return new OtpVerificationResult(true, clocks[i].getOffset());
			}
		}

		return new OtpVerificationResult(false, 0);
	}

	// size MUST be odd
	private IncrementalClock[] getClocks(int initialOffset, int size) {
		IncrementalClock[] clocks = new IncrementalClock[size];
		
		int cut = (size - 1) / 2;
		for (int i = (cut * - 1), j = 0; j < size; i++, j++) {
			clocks[j] = new IncrementalClock(initialOffset + i * 30);
		}
		
		return clocks;
	}

	private boolean isValidLong(String code) {
		try {
			Long.parseLong(code);
		}
		catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}
