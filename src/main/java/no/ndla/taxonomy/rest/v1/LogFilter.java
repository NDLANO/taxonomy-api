package no.ndla.taxonomy.rest.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

import static java.lang.System.currentTimeMillis;

@Component
@Order(1)
public class LogFilter extends GenericFilterBean {

    private static final Logger LOGGER = LoggerFactory.getLogger("accesslog");


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!LOGGER.isInfoEnabled()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        try {
            doFilterWithLogging((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
        } finally {
            MDC.clear();
        }
    }

    private void doFilterWithLogging(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        try {
            MDC.put("remote_addr", request.getRemoteAddr());
            MDC.put("request_url", String.valueOf(request.getRequestURL()));
            MDC.put("x-consumer-username", request.getHeader("x-consumer-username"));
            MDC.put("x-consumer-id", request.getHeader("x-consumer-id"));
            MDC.put("x-correlation-id", request.getHeader("x-correlation-id"));

        } catch (Exception e) {
            //Don't fail the user request if the log system is broken
            e.printStackTrace();
        }

        long starts = currentTimeMillis();
        filterChain.doFilter(request, response);

        try {
            MDC.put("completion_time", String.valueOf(currentTimeMillis() - starts));
            MDC.put("http_status", String.valueOf(response.getStatus()));

            if (LOGGER.isDebugEnabled()) {
                String headers = extractHeaders(request);
                MDC.put("headers", headers);
            }

            if (response.getStatus() == HttpServletResponse.SC_CREATED) {
                MDC.put("path_location", response.getHeader("Location"));
            }

            LOGGER.info(request.getMethod() + " " + request.getRequestURL());
        } catch (Exception e) {
            //Don't fail the user request if the log system is broken
            e.printStackTrace();
        }
    }



    private String extractHeaders(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headers = request.getHeaders(headerName);
            while (headers.hasMoreElements()) {
                String value = headers.nextElement();
                sb.append(headerName).append(": ").append(value).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public void destroy() {

    }
}
