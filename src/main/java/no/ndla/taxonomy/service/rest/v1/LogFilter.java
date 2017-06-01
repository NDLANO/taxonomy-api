package no.ndla.taxonomy.service.rest.v1;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

@Configuration
public class LogFilter implements Filter {

    Logger logger = LoggerFactory.getLogger("accesslog");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!logger.isInfoEnabled()) {
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

            //Should be in doFilter, we always want this info
            String authorizationHeader = request.getHeader("authorization");
            DecodedJWT jwt = JWT.decode(authorizationHeader.substring(6));
            MDC.put("JWT-signature", jwt.getSignature());
            MDC.put("JWT-iat", jwt.getIssuedAt().toString());
            MDC.put("JWT-exp", jwt.getExpiresAt().toString());
            MDC.put("JWT-iss", jwt.getIssuer());
            Map<String, Claim> claims = jwt.getClaims();
            System.out.println("claims: " + claims.keySet().toString());
            Claim app_metadata = claims.get("app_metadata");
            Map<String, Object> appMetadataMap = app_metadata.asMap();
            System.out.println("metadata keys: " + appMetadataMap.keySet().toString());
            Object roles = appMetadataMap.get("roles");
            System.out.println("roles: " + roles.toString());
            MDC.put("roles", roles.toString());
            String decoded = StringUtils.newStringUtf8(Base64.getDecoder().decode(jwt.getPayload()));
            MDC.put("JWT-payload", decoded);

        } catch (Exception e) {
            //Don't fail the user request if the log system is broken
            e.printStackTrace();
        }

        long starts = currentTimeMillis();
        filterChain.doFilter(request, response);

        try {
            MDC.put("completion_time", String.valueOf(currentTimeMillis() - starts));
            MDC.put("http_status", String.valueOf(response.getStatus()));

            if (logger.isDebugEnabled()) {
                String headers = extractHeaders(request);
                MDC.put("headers", headers);
            }

            if (response.getStatus() == HttpServletResponse.SC_CREATED) {
                MDC.put("path_location", response.getHeader("Location"));
            }

            logger.info(request.getMethod() + " " + request.getRequestURL());
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
