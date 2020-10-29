/**
 * Copyright (c) 2010, DanID A/S
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of the DanID A/S nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dk.nemid.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.openoces.ooapi.web.Signer;

public class OtpClientGenerator {
	private List<Parameter> params = new ArrayList<Parameter>();
	private Signer signer;

	public OtpClientGenerator(HttpServletRequest request, String origin) {
		String url = NemIdProperties.getAppletParameterSigningKeystore();
		String keyStorePw = NemIdProperties.getAppletParameterSigningKeystorePassword();

		signer = new Signer(url, keyStorePw);

		try {
			setClientFlow(request, origin);
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to set client flow", e);
		}
	}

	private void setClientFlow(HttpServletRequest request, String origin) throws Exception {
		if (origin == null) {
			origin = generateOrigin(request);
		}

		params.add(new ParameterImpl("TIMESTAMP", getTimestamp(), true));
		params.add(new ParameterImpl("SP_CERT", signer.getCertificate()));
		params.add(new ParameterImpl("SIGN_PROPERTIES", "challenge=" + ChallengeGenerator.generateChallenge(request.getSession())));
		params.add(new ParameterImpl("ORIGIN", origin, false));
		params.add(new ParameterImpl("CLIENTFLOW", "OCESLOGIN2"));

		String normalizedParameters = ParameterImpl.normalize(params);

		params.add(new ParameterImpl("PARAMS_DIGEST", ParameterImpl.computeDigest(normalizedParameters)));
		params.add(new ParameterImpl("DIGEST_SIGNATURE", computeSignature(normalizedParameters)));
	}

	public String getJSElement() {
		StringBuffer sb = new StringBuffer();
		sb.append("<script type=\"text/x-nemid\" id=\"nemid_parameters\">\r\n");
		sb.append(ParameterImpl.toJson(params).toString()).append("\r\n");
		sb.append("</script>\r\n");

		return sb.toString();
	}

	private String computeSignature(String normalizedParameters) {
		byte[] signedBytes = signer.calculateSignature(normalizedParameters.getBytes());

		return new String(Base64.encodeBase64(signedBytes));
	}

	private String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		long millis = System.currentTimeMillis();

		return sdf.format(new Date(millis));
	}

	private String generateOrigin(HttpServletRequest request) {
		int port = request.getServerPort();

		if (request.getScheme().equals("http") && port == 80) {
			port = -1;
		}
		else if (request.getScheme().equals("https") && port == 443) {
			port = -1;
		}

		try {
			URL serverURL = new URL(request.getScheme(), request.getServerName(), port, "");
			return serverURL.toString();
		}
		catch (MalformedURLException me) {
			throw new RuntimeException(me);
		}
	}
}
