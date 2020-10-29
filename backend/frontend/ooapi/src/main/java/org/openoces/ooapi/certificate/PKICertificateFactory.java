package org.openoces.ooapi.certificate;

import java.security.cert.X509Certificate;
import java.util.List;

import org.openoces.ooapi.exceptions.TrustCouldNotBeVerifiedException;

public interface PKICertificateFactory {

	Certificate generate(List<X509Certificate> certificates) throws TrustCouldNotBeVerifiedException;
	

}
