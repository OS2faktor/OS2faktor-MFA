package dk.digitalidentity.os2faktor.service;

import java.nio.charset.Charset;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openoces.ooapi.certificate.CertificateStatus;
import org.openoces.ooapi.certificate.PocesCertificate;
import org.openoces.securitypackage.LogonHandler;
import org.openoces.serviceprovider.CertificateAndStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import dk.digitalidentity.nemid.Pid2Cpr;
import dk.digitalidentity.os2faktor.service.model.PidAndCprOrError;
import dk.nemid.common.ChallengeGenerator;
import dk.nemid.common.NemIdProperties;
import dk.nemid.common.OcesEnvironment;
import dk.nemid.common.OtpClientGenerator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NemIDService {

	@Autowired
	private Pid2Cpr pid2Cpr;

	static {
		OcesEnvironment.setOcesEnvironment();	
	}

	public void populateModel(Model model, HttpServletRequest request) {
		OtpClientGenerator paramGen = new OtpClientGenerator(request, getOrigin(request));

		model.addAttribute("jsElement", paramGen.getJSElement());
		model.addAttribute("serverUrlPrefix", NemIdProperties.getServerUrlPrefix());

		StringBuffer sb = new StringBuffer();
		sb.append(NemIdProperties.getServerUrlPrefix());
		sb.append("/launcher/lmt/");
		sb.append(System.currentTimeMillis());

		model.addAttribute("iframeSrc", sb.toString());
	}
	
	public PidAndCprOrError verify(String responseB64, HttpServletRequest request) {
		String signature = null;
		String result = null;

		if (responseB64 != null && responseB64.length() > 0) {
			String decodedResponse = new String(Base64.getDecoder().decode(responseB64), Charset.forName("UTF-8"));

			if (decodedResponse.length() > 20) {
				result = "ok";
				signature = decodedResponse;
			}
			else {
				result = decodedResponse;
			}
		}

		if ("ok".equals(result)) {
			try {
				HttpSession httpSession = request.getSession();

				String challenge = ChallengeGenerator.getChallenge(httpSession);
				String serviceProviderName = NemIdProperties.getServiceProviderName();

				CertificateAndStatus certificateAndStatus = LogonHandler.validateAndExtractCertificateAndStatus(signature, challenge, serviceProviderName);

				if (certificateAndStatus.getCertificateStatus() != CertificateStatus.VALID) {
					return new PidAndCprOrError("Certificate status is: " + certificateAndStatus.getCertificateStatus());
				}
				else if (certificateAndStatus.getCertificate() instanceof PocesCertificate) {
		            PocesCertificate pocesCert = ((PocesCertificate)certificateAndStatus.getCertificate());
		            String pid = pocesCert.getPid();
		            String cpr = pid2Cpr.lookup(pid);
		            
		            if (cpr == null || cpr.length() == 0) {
		            	return new PidAndCprOrError("Unable to get CPR for pid: " + pid);
		            }

		            return new PidAndCprOrError(pid, cpr);
				}

	           	return new PidAndCprOrError("Certificate is not FOCES");
			}
			catch (Exception ex) {
				log.error("Failure during NemID validation", ex);
				
            	return new PidAndCprOrError(ex.getMessage());
			}
		}

		log.error("NemID validation failed: " + result);

		return new PidAndCprOrError(result);
	}
	
	private String getOrigin(HttpServletRequest request) {
		String domain = request.getHeader("host");
		String protocol = request.getHeader("x-forwarded-proto");
		
		// if we are behind a load balancer do this
		if (domain != null && protocol != null) {
			return protocol + "://" + domain;
		}
		
		// otherwise just use the request data
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
	}
}
