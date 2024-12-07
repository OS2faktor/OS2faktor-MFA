package dk.digitalidentity.os2faktor.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.service.HashingService;

@Configuration
public class SecurityFilterConfiguration {
	
	@Autowired
	private ClientDao clientDao;

	@Autowired
	private HashingService hashingService;

	@Bean
	public FilterRegistrationBean<ClientSecurityFilter> clientSecurityFilter() {
		ClientSecurityFilter filter = new ClientSecurityFilter();
		filter.setClientDao(clientDao);
		filter.setHashingService(hashingService);

		FilterRegistrationBean<ClientSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/ui/selfservice", "/ui/selfservice/*", "/ui/pin/*");

		return filterRegistrationBean;
	}
}
