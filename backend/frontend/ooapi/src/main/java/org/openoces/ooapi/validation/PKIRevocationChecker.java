package org.openoces.ooapi.validation;

import org.openoces.ooapi.certificate.Certificate;

import java.security.cert.X509CRLEntry;

public interface PKIRevocationChecker {
    public RevocationStatus isRevoked(Certificate certificate);
    public X509CRLEntry getRevocationDetails(Certificate certificate);
}