package dk.digitalidentity.os2faktor.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;

public class ClientSecurityFilter implements Filter {
	public static final String SESSION_ERROR = "SESSION_ERROR";
	public static final String SESSION_CLIENT = "SESSION_CLIENT";
	public static final String SESSION_USER = "SESSION_USER";
	public static final String SESSION_DEVICE_ID = "SESSION_DEVICE_ID";
	public static final String SESSION_TYPE = "SESSION_TYPE";
	public static final String SESSION_TOKEN = "SESSION_TOKEN";
	public static final String SESSION_TEST_LOGIN = "SESSION_TEST_LOGIN";

	private ClientDao clientDao;

	public void setClientDao(ClientDao clientDao) {
		this.clientDao = clientDao;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;

		// if not authenticated, authenticate, otherwise just pass-through
		if (request.getSession().getAttribute(SESSION_CLIENT) == null) {
			String apiKey = request.getParameter("apiKey");
			String deviceId = request.getParameter("deviceId");

			if (apiKey != null && deviceId != null) {
				Client client = clientDao.getByDeviceId(deviceId);

				if (client == null) {
					request.getSession().setAttribute(SESSION_ERROR, ErrorType.UNKNOWN_CLIENT);
					request.getSession().removeAttribute(SESSION_CLIENT);
				}
				else if (!client.getApiKey().equals(apiKey)) {
					request.getSession().setAttribute(SESSION_ERROR, ErrorType.BAD_CREDENTIALS);
					request.getSession().removeAttribute(SESSION_CLIENT);
				}
				else {
					// force load everything (hibernate proxy hack)
					if (client.getUser() != null && client.getUser().getClients() != null) {
						client.getUser().getClients().size();
					}

					request.getSession().setAttribute(SESSION_CLIENT, client);
				}
			}
			else {
				request.getSession().setAttribute(SESSION_ERROR, ErrorType.UNKNOWN_CLIENT);
				request.getSession().removeAttribute(SESSION_CLIENT);
			}
		}

		filterChain.doFilter(servletRequest, servletResponse);
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
