/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

public class JWTAuthentication implements Authentication {

    private static final long serialVersionUID = 1L;
    private static final String TAXONOMY_API = "taxonomy";
    private static final String WRITE_PERMISSION = "write";
    private static final String PRODUCTION = "prod";

    private Collection<GrantedAuthority> authorities;
    private boolean authenticated;
    private Map<String, Claim> claims;
    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthentication.class);

    public JWTAuthentication(DecodedJWT token) {
        Claim appMetadata = token.getClaim("scope");

        List<GrantedAuthority> tmp = new ArrayList<>();
        tmp.add(new SimpleGrantedAuthority("READONLY"));

        try {
            if (appMetadata != null && appMetadata.asString() != null) {
                final String[] allPermissions = appMetadata.asString().split(" ");
                for (String jwtPermissionString : allPermissions) {
                    final JWTPermission jwtPermission = new JWTPermission(jwtPermissionString);
                    if (jwtPermission.getApi() != null && jwtPermission.getPermission() != null
                            && jwtPermission.getApi().equals(TAXONOMY_API)
                            && jwtPermission.getPermission().equals(WRITE_PERMISSION)) {
                        tmp.add(new SimpleGrantedAuthority("TAXONOMY_WRITE"));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("JWT Authentication failed", e);
        }

        this.authorities = Collections.unmodifiableList(tmp);
        this.claims = token.getClaims();
        authenticated = true;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return new Object();
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getDetails() {
        return claims;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }
}
