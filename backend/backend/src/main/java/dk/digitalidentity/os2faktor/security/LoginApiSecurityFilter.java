package dk.digitalidentity.os2faktor.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.digitalidentity.os2faktor.dao.model.LoginServiceProvider;
import dk.digitalidentity.os2faktor.service.LoginServiceProviderService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginApiSecurityFilter implements Filter {
	private LoginServiceProviderService loginServiceProviderService;

	public void setLoginServiceProviderService(LoginServiceProviderService loginServiceProviderService) {
		this.loginServiceProviderService = loginServiceProviderService;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		// we are using a custom header instead of Authorization because
		// the Authorization header is already handled by Spring Security
		String apiKey = request.getHeader("ApiKey");
		if (apiKey != null) {
			LoginServiceProvider loginServiceProvider = loginServiceProviderService.getByApiKey(apiKey);
			if (loginServiceProvider == null) {
				log.info("LoginApiSecurityFilter: Invalid ApiKey Header");
				response.sendError(401, "Invalid ApiKey Header");
				return;
			}

			try {
				AuthorizedLoginServiceProviderHolder.setLoginServiceProvider(loginServiceProvider);

				filterChain.doFilter(servletRequest, servletResponse);
			}
			finally {
				AuthorizedLoginServiceProviderHolder.clear();
			}
		}
		else {
			log.info("LoginApiSecurityFilter: Missing ApiKey Header");
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
