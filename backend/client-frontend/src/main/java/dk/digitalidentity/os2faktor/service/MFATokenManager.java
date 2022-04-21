package dk.digitalidentity.os2faktor.service;

import java.io.ByteArrayOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

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

	public boolean verifyTotp(String code, String secret) {
		code = code.replace(" ", "");
		Totp totp = new Totp(secret);
		if (isValidLong(code)) {
			return totp.verify(code);
		}

		return false;
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
