package dk.digitalidentity.os2faktor.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.digitalidentity.os2faktor.dao.ClientDao;

@Configuration
public class SecurityFilterConfiguration {
	
	@Autowired
	private ClientDao clientDao;

	@Bean
	public FilterRegistrationBean<ClientSecurityFilter> clientSecurityFilter() {
		ClientSecurityFilter filter = new ClientSecurityFilter();
		filter.setClientDao(clientDao);

		FilterRegistrationBean<ClientSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/ui/selfservice", "/ui/selfservice/*", "/ui/pin/*");

		return filterRegistrationBean;
	}
}
