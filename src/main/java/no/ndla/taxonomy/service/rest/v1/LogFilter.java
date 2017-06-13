package no.ndla.taxonomy.service.rest.v1;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import no.ndla.taxonomy.service.JWTToken;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Map;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Configuration
public class LogFilter extends GenericFilterBean {

    private final AuthenticationManager authenticationManager;

    Logger logger = LoggerFactory.getLogger("accesslog");

    //@Value("${jwt.header}")
    //private String tokenHeader;

    public LogFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
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
            parseWebToken(request);
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

    private void parseWebToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("authorization");
        if (isBlank(authorizationHeader)) return;
        if (!authorizationHeader.startsWith("Bearer")) return;

        DecodedJWT jwt = JWT.decode(authorizationHeader.substring(6));
        JWTToken token = new JWTToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(token);
        Map<String, Claim> claims = jwt.getClaims();
        Claim appMetadata = claims.get("app_metadata");
        if (null == appMetadata) return;

        Map<String, Object> appMetadataMap = appMetadata.asMap();

        Object roles = appMetadataMap.get("roles");
        MDC.put("roles", "" + roles);

        String payload = StringUtils.newStringUtf8(Base64.getDecoder().decode(jwt.getPayload()));
        MDC.put("JWT-payload", payload);
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
