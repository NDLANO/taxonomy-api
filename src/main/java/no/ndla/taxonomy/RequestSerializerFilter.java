package no.ndla.taxonomy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@Order(1)
@ConditionalOnProperty(name = "requestqueue.enabled", value = "true")
public class RequestSerializerFilter extends GenericFilterBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestSerializerFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper((HttpServletRequest) servletRequest);
        System.out.println("query string: " + req.getQueryString());
        System.out.println("url: " + req.getRequestURL().toString());
        System.out.println("method: " + req.getMethod());
        System.out.println("contenttype: " + req.getContentType());
        filterChain.doFilter(req, servletResponse);
        System.out.println("body: " + new String(req.getContentAsByteArray(), "UTF-8"));
    }

}
