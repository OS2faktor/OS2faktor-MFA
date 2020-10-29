package dk.digitalidentity.os2faktor.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.MunicipalityDao;
import dk.digitalidentity.os2faktor.dao.ServerDao;

@Configuration
public class ApiSecurityFilterConfiguration {

	@Autowired
	private ServerDao serverDao;
	
	@Autowired
	private ClientDao clientDao;

	@Autowired
	private MunicipalityDao municipalityDao;

	@Bean
	public FilterRegistrationBean<ServerApiSecurityFilter> serverApiSecurityFilter() {
		ServerApiSecurityFilter filter = new ServerApiSecurityFilter();
		filter.setServerDao(serverDao);

		FilterRegistrationBean<ServerApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/api/server/*");

		return filterRegistrationBean;
	}
	
	@Bean
	public FilterRegistrationBean<ClientApiSecurityFilter> clientApiSecurityFilter() {
		ClientApiSecurityFilter filter = new ClientApiSecurityFilter();
		filter.setClientDao(clientDao);

		FilterRegistrationBean<ClientApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/api/client/*");

		return filterRegistrationBean;
	}
	
	@Bean
	public FilterRegistrationBean<MunicipalityApiSecurityFilter> municipalityApiSecurityFilter() {
		MunicipalityApiSecurityFilter filter = new MunicipalityApiSecurityFilter();
		filter.setMunicipalityDao(municipalityDao);

		FilterRegistrationBean<MunicipalityApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/api/municipality/*");

		return filterRegistrationBean;
	}
}
