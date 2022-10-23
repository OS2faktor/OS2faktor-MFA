package dk.digitalidentity.os2faktor.security;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.digitalidentity.os2faktor.dao.ServerDao;
import dk.digitalidentity.os2faktor.dao.model.Server;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerApiSecurityFilter implements Filter {
	private ServerDao serverDao;

	public void setServerDao(ServerDao serverDao) {
		this.serverDao = serverDao;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		// we are using a custom header instead of Authorization because
		// the Authorization header is already handled by Spring Security
		String apiKey = request.getHeader("ApiKey");
		if (apiKey != null) {

			Server server = null;
			try {
				server = serverDao.getByApiKey(apiKey);
			}
			catch (Exception ex) {
				throw new ServletException(ex);
			}

			if (server == null) {
				log.info("Invalid ApiKey Header");
				response.sendError(401, "Invalid ApiKey Header");
				return;
			}

			try {
				String tlsVersion = request.getHeader("x-amzn-tls-version");
				tlsVersion = (tlsVersion != null) ? ((tlsVersion.length() > 64) ? (tlsVersion.substring(0, 60) + "...") : tlsVersion) : null;

				if (tlsVersion != null && !Objects.equals(server.getTlsVersion(), tlsVersion)) {
					server.setTlsVersion(tlsVersion);
					server = serverDao.save(server);
				}

				AuthorizedServerHolder.setServer(server);

				filterChain.doFilter(servletRequest, servletResponse);
			}
			finally {
				AuthorizedServerHolder.clear();
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
