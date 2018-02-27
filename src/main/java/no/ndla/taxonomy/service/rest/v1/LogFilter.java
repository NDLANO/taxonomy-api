package no.ndla.taxonomy.service.rest.v1;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import no.ndla.taxonomy.service.JWTToken;
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
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Configuration
public class LogFilter extends GenericFilterBean {

    private final AuthenticationManager authenticationManager;

    Logger logger = LoggerFactory.getLogger("accesslog");

    @Value(value = "${auth0.issuer}")
    private String issuer;

    @Value(value="${auth0.jwks.kid}")
    private String kid;

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

    private void parseWebToken(HttpServletRequest request) throws JwkException {
        String authorizationHeader = request.getHeader("authorization");
        if (isBlank(authorizationHeader)) return;
        if (!authorizationHeader.startsWith("Bearer")) return;

        try {
            DecodedJWT jwt = verifyWebToken(authorizationHeader.substring(6).trim());

            JWTToken token = new JWTToken(jwt);
            SecurityContextHolder.getContext().setAuthentication(token);
         } catch (JwkException e) {
            System.out.println("No JWKs to verify against.");
            throw e;
        }

    }

    private DecodedJWT verifyWebToken(String token) throws JwkException {
        JwkProvider provider = new UrlJwkProvider(issuer);
        Jwk jwk = provider.get(kid);
        RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

        try {
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();

            final DecodedJWT decoded = verifier
                .verify(token);
            return decoded;
        } catch (JWTVerificationException exception) {
            throw exception;
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
