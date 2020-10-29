package org.openoces.ooapi.validation;

import java.security.cert.X509Certificate;

public interface CaCrlRevokedChecker extends RevocationChecker {
    boolean isRevokedCa(X509Certificate caCertificate);    
}
