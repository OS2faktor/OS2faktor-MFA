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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openoces.ooapi.environment.Environments;

public class NemIdProperties {
	private static String serverUrlPrefix;
	private static String appletParameterSigningKeystore;
	private static String appletParameterSigningKeystorePassword;
	private static String serviceProviderName;
	private static Environments.Environment oces2Environment;

	static {
		readProperties();
	}

	private static void readProperties() {
		Properties properties = new Properties();
		
		try {
			InputStream propertiesAsStream = NemIdProperties.class.getResourceAsStream("/nemid.properties");
			
			if (propertiesAsStream == null) {
				throw new IllegalStateException("/nemid.properties not found on classpath");
			}
			
			properties.load(propertiesAsStream);
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not read property file nemid.properties from classpath", e);
		}

		serverUrlPrefix = getRequiredProperty(properties, "nemid.applet.server.url.prefix", " to the URL of the applet providing server, eg. https://applet.danid.dk");
		appletParameterSigningKeystore = getRequiredProperty(properties, "nemid.applet.parameter.signing.keystore", " to a classpath path to the keystore, eg. /applet-parameter-signing-keystore-cvr30808460-uid1263281782319.jks");
		appletParameterSigningKeystorePassword = getRequiredProperty(properties, "nemid.applet.parameter.signing.keystore.password", " to the password to the keystore pointed to by nemid.applet.parameter.signing.keystore");
		serviceProviderName = getRequiredProperty(properties, "nemid.serviceprovider.logonto", " to you service provider name");
		oces2Environment = getOces2EnvironmentFromProperty(properties, "oces2.environment");
	}

	private static Environments.Environment getOces2EnvironmentFromProperty(Properties properties, String s) {
		return Environments.Environment.valueOf(getRequiredProperty(properties, s, " to the environment to check nemid environment against, eg. OCESII_DANID_ENV_PROD, " + "OCESII_DANID_ENV_PROD or OCESII_DANID_ENV_EXTERNALTEST").toUpperCase());
	}

	private static String getRequiredProperty(Properties properties, String key, String helpMsg) {
		String value = properties.getProperty(key);

		if (value == null || value.length() == 0) {
			throw new IllegalStateException("You must set property " + key + " in nemid.properties" + helpMsg);
		}
		
		return value;
	}

	public static String getServerUrlPrefix() {
		return serverUrlPrefix;
	}

	public static String getServiceProviderName() {
		return serviceProviderName;
	}

	public static Environments.Environment getOces2Environment() {
		return oces2Environment;
	}

	public static String getAppletParameterSigningKeystore() {
		return appletParameterSigningKeystore;
	}

	public static String getAppletParameterSigningKeystorePassword() {
		return appletParameterSigningKeystorePassword;
	}
}
