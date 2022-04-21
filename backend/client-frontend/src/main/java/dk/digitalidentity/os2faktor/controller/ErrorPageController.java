package dk.digitalidentity.os2faktor.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class ErrorPageController implements ErrorController {
	private ErrorAttributes errorAttributes = new DefaultErrorAttributes();

	@RequestMapping(value = "/error", produces = "text/html")
	public String errorPage(Model model, HttpServletRequest request) {
		boolean mobile = false;

		// grab exception (if any)
		Object authException = request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

		// handle the forward case
		if (authException == null && request.getSession() != null) {
			authException = request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}

		// find root-exception
		Throwable t = null;
		if (authException != null && authException instanceof Throwable) {
			t = (Throwable) authException;

			while (t.getCause() != null) {
				t = t.getCause();
			}
		}

		if (t != null) {		
			if (t instanceof AuthenticationException) {
				model.addAttribute("exception", t);

				log.warn("AuthenticationException: " + t.getMessage());
			}
			else {
				log.error("Unexpected error", t);
			}
		}

		// check for mobile optimized pages (so error page can be mobile optimized)
		Object originalUri = request.getAttribute("javax.servlet.forward.request_uri");
		if (originalUri instanceof String) {
			String uri = (String) originalUri;
			
			if (uri.startsWith("/ui")) {
				mobile = true;
				model.addAttribute("mobile", true);
			}
		}
		
		Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));
		model.addAllAttributes(body);
		
		if (mobile) {
			return "defaulterror";
		}
		
		return "desktop/error";
	}

	@RequestMapping(value = "/error", produces = "application/json")
	public ResponseEntity<Map<String, Object>> errorJSON(HttpServletRequest request) {
		Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		try {
			status = HttpStatus.valueOf((int) body.get("status"));
		}
		catch (Exception ex) {
			;
		}

		return new ResponseEntity<>(body, status);
	}

	private Map<String, Object> getErrorAttributes(WebRequest request) {
		return errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
	}
}
