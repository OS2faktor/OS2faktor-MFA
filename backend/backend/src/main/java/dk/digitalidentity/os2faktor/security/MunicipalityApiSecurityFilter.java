package dk.digitalidentity.os2faktor.security;

import java.io.IOException;

import dk.digitalidentity.os2faktor.dao.MunicipalityDao;
import dk.digitalidentity.os2faktor.dao.model.Municipality;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MunicipalityApiSecurityFilter implements Filter {
	private MunicipalityDao municipalityDao;

	public void setMunicipalityDao(MunicipalityDao municipalityDao) {
		this.municipalityDao = municipalityDao;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		// we are using a custom header instead of Authorization because
		// the Authorization header is already handled by Spring Security
		String apiKey = request.getHeader("ApiKey");
		if (apiKey != null) {
			Municipality municipality = municipalityDao.findByApiKey(apiKey);
			if (municipality == null) {
				log.info("Invalid ApiKey Header");
				response.sendError(401, "Invalid ApiKey Header");

				return;
			}

			try {
				AuthorizedMunicipalityHolder.setMunicipality(municipality);

				filterChain.doFilter(servletRequest, servletResponse);
			}
			finally {
				AuthorizedMunicipalityHolder.clear();
			}
		}
		else {
			log.info("Missing ApiKey Header");
			response.sendError(401, "Missing ApiKey Header");
		}
	}

	@Override
	public void destroy() {
		;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		;
	}
}
