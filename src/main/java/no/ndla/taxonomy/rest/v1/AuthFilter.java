package no.ndla.taxonomy.rest.v1;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import no.ndla.taxonomy.JWTAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class AuthFilter extends GenericFilterBean {

    @Value(value = "${auth0.issuer}")
    private String issuer;

    @Value(value = "${auth0.jwks.kid}")
    private String kid;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            parseWebToken((HttpServletRequest) servletRequest);
        } catch (JwkException e) {
            LOGGER.error("Error when parsing JWT Token", e);
            throw new ServletException(e);
        } finally {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private void parseWebToken(HttpServletRequest request) throws JwkException {
        String authorizationHeader = request.getHeader("authorization");
        if (isBlank(authorizationHeader) || (!authorizationHeader.startsWith("Bearer"))) {
            return;
        }
        DecodedJWT decodedToken = verifyWebToken(authorizationHeader.substring(6).trim());
        JWTAuthentication jwtAuthentication = new JWTAuthentication(decodedToken);
        SecurityContextHolder.getContext().setAuthentication(jwtAuthentication);
    }

    private DecodedJWT verifyWebToken(String token) throws JwkException {
        JwkProvider provider = new UrlJwkProvider(issuer);
        Jwk jwk = provider.get(kid);
        RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

        try {
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).build();
            return verifier.verify(token);
        } catch (JWTVerificationException exception) {
            MDC.put("Exception in verification: ", exception.toString());
            throw exception;
        }
    }

}
