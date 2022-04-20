package dk.digitalidentity.os2faktor.security;

import dk.digitalidentity.os2faktor.service.HashingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.MunicipalityDao;
import dk.digitalidentity.os2faktor.dao.ServerDao;
import dk.digitalidentity.os2faktor.service.LoginServiceProviderService;

@Configuration
public class ApiSecurityFilterConfiguration {

	@Autowired
	private ServerDao serverDao;
	
	@Autowired
	private ClientDao clientDao;

	@Autowired
	private HashingService hashingService;

	@Autowired
	private MunicipalityDao municipalityDao;

	@Autowired
	private LoginServiceProviderService loginServiceProviderService;

	@Bean
	public FilterRegistrationBean<LoginApiSecurityFilter> loginApiSecurityFilter() {
		LoginApiSecurityFilter filter = new LoginApiSecurityFilter();
		filter.setLoginServiceProviderService(loginServiceProviderService);

		FilterRegistrationBean<LoginApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/api/login/*");

		return filterRegistrationBean;
	}

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
		filter.setHashingServiceDao(hashingService);

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
