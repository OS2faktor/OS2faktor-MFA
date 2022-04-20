package dk.digitalidentity.os2faktor.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dk.digitalidentity.os2faktor.config.Constants;

public class SecurityUtil {
	
	public static String getSAMAccountName() {
		HttpServletRequest request = getRequest();
		if (request != null) {
			Object o = request.getSession().getAttribute(Constants.SESSION_SAMACCOUNTNAME);
			if (o == null || !(o instanceof String)) {
				return null;
			}
	
			return (String) o;
		}

		return null;
	}
	
	public static String getSsn() {
		HttpServletRequest request = getRequest();
		if (request != null) {
			Object o = request.getSession().getAttribute(Constants.SESSION_SSN);
			if (o == null || !(o instanceof String)) {
				return null;
			}
	
			return (String) o;
		}

		return null;
	}
	
	public static void logout() {
		HttpServletRequest request = getRequest();
		if (request != null) {
			request.getSession().removeAttribute(Constants.SESSION_SSN);
			request.getSession().removeAttribute(Constants.SESSION_SAMACCOUNTNAME);
			request.getSession().removeAttribute(Constants.SESSION_SAMACCOUNTNAMES);
			request.getSession().removeAttribute(Constants.SESSION_DESKTOP);
		}
	}

	private static HttpServletRequest getRequest() {
		try {
			return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}
}
