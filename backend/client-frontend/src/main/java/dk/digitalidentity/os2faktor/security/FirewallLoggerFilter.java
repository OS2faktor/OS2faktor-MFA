package dk.digitalidentity.os2faktor.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FirewallLoggerFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(req, res);
        }
        catch (RequestRejectedException ex) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            log.warn("request_rejected: remote={}, user_agent={}, request_url={}",
	            request.getRemoteHost(),
	            request.getHeader(HttpHeaders.USER_AGENT),
	            request.getRequestURL()
            );

            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}

