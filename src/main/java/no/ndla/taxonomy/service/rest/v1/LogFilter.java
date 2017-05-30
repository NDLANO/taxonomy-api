package no.ndla.taxonomy.service.rest.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.lang.System.currentTimeMillis;

@Configuration
public class LogFilter implements Filter {

    Logger logger = LoggerFactory.getLogger("accesslog");

    @Value("${service:ndla-taxonomy-local}")
    private String service;

    public LogFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        long starts = currentTimeMillis();
        filterChain.doFilter(servletRequest, servletResponse);
        long ends = currentTimeMillis();

        MDC.put("service", service);
        MDC.put("remote_addr", request.getRemoteAddr());
        MDC.put("completion_time", String.valueOf(ends - starts));
        MDC.put("http_status", String.valueOf(response.getStatus()));
        MDC.put("request_url", String.valueOf(request.getRequestURL()));

        if (response.getStatus() == HttpServletResponse.SC_CREATED) {
            MDC.put("path_location", response.getHeader("Location"));
        }
        logger.info(request.getMethod() + " " + request.getRequestURL());
    }

    @Override
    public void destroy() {

    }
}
