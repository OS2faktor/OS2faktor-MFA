package org.openoces.ooapi.environment;

import org.openoces.ooapi.certificate.PKICertificateFactory;
import org.openoces.ooapi.validation.PKIRevocationChecker;

public interface PKIEnvironment extends Comparable<PKIEnvironment> {
	public PKICertificateFactory getPKICertificateFactory();
	public PKIRevocationChecker getPKIRevocationChecker();
}
