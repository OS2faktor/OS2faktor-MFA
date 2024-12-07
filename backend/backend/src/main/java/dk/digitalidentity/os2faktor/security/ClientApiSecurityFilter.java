package dk.digitalidentity.os2faktor.security;

import java.io.IOException;

import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.service.HashingService;
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
public class ClientApiSecurityFilter implements Filter {
	private ClientDao clientDao;
	private HashingService hashingService;

	public void setClientDao(ClientDao clientDao) {
		this.clientDao = clientDao;
	}

	public void setHashingServiceDao(HashingService hashingService) {
		this.hashingService = hashingService;
	}
	
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		
		// allow CORS requests to pass-through
		if (request.getMethod().equals("OPTIONS")) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}

		// we are using a custom header instead of Authorization because
		// the Authorization header is already handled by Spring Security
		String apiKey = request.getHeader("ApiKey");
		String deviceId = request.getHeader("deviceId");

		if (apiKey != null && deviceId != null) {
			Client client = clientDao.findByDeviceId(deviceId);
			if (client == null) {
				log.debug("No client with deviceId: " + deviceId);
				response.sendError(401, "No client with deviceId: " + deviceId);
				return;
			}

			boolean matches = false;
			try {
				matches = hashingService.matches(apiKey, client.getApiKey());
			}
			catch (Exception ex) {
				log.error("Failed to check matching apiKeys", ex);
				response.sendError(500, "Failed to check matching apiKeys");
				return;
			}

			if (!matches) {
				log.error("Wrong ApiKey on: " + deviceId);
				response.sendError(401, "Wrong ApiKey!");
				return;
			}

			filterChain.doFilter(servletRequest, servletResponse);
		}
		else {
			log.info("Missing deviceId or ApiKey Header: " + deviceId);
			response.sendError(401, "Missing deviceId or ApiKey Header");
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
