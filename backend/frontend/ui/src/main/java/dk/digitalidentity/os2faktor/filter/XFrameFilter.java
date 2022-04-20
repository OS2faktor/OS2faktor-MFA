package dk.digitalidentity.os2faktor.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class XFrameFilter implements Filter {
	private static final String XFRAME_HEADER = "x-frame-options";
    
    public void init(FilterConfig filterConfig) throws ServletException {
    	;
    }
    
    public void destroy() {
    	;
    }

    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            chain.doFilter(request, new XFrameWrapper((HttpServletResponse) response));
        }
        else {
        	chain.doFilter(request, response);
        }
    }
    
    private class XFrameWrapper extends HttpServletResponseWrapper {

    	public XFrameWrapper(HttpServletResponse resp) {
            super(resp);
        }
        
        @Override
        public void addHeader(String name, String value) {
        	if (name.equalsIgnoreCase(XFRAME_HEADER)) {
        		return;
        	}

        	super.addHeader(name, value);
        }
        
        @Override
        public void setHeader(String name, String value) {
        	if (name.equalsIgnoreCase(XFRAME_HEADER)) {
        		return;
        	}

        	super.setHeader(name, value);
        }        
    }
}