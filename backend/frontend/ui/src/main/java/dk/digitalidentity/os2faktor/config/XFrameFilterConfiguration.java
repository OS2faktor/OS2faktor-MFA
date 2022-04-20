package dk.digitalidentity.os2faktor.config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.digitalidentity.os2faktor.filter.XFrameFilter;

@Configuration
public class XFrameFilterConfiguration {

	@Bean
	public FilterRegistrationBean<XFrameFilter> xFrameFilter() {
	    List<String> urlPatterns = new ArrayList<>();
	    urlPatterns.add("/ui/yubikey/*");

		FilterRegistrationBean<XFrameFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new XFrameFilter());
		registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
		registration.setUrlPatterns(urlPatterns);
		registration.setOrder(Integer.MIN_VALUE);

		return registration;
	}
}
