package no.ndla.taxonomy.service;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

public class JWTToken implements Authentication {

    private static final long serialVersionUID = 1L;

    private DecodedJWT jwt;
    private Collection<GrantedAuthority> authorities;
    private boolean authenticated;
    private Map<String, Claim> claims;

    public JWTToken(DecodedJWT jwt) {

        this.jwt = jwt;
        List<String> roles;
        Map<String, Claim> claims = jwt.getClaims();
        Claim appMetadata = claims.get("app_metadata");
        if (null == appMetadata) return;

        Map<String, Object> appMetadataMap = appMetadata.asMap();


        Object roleMap = appMetadataMap.get("roles");
        if (roleMap == null) {
            roles = new ArrayList<>();
        } else {
            roles = (List<String>) roleMap;
        }


        List<GrantedAuthority> tmp = new ArrayList<>();
        /*for (String role : roles) {
            tmp.add(new SimpleGrantedAuthority(role));
        }*/
        tmp.add(new SimpleGrantedAuthority("READONLY"));
        this.authorities = Collections.unmodifiableList(tmp);
        SimpleGrantedAuthority bah = new SimpleGrantedAuthority("READONLY");
        System.out.println(bah.getAuthority());
        this.claims = jwt.getClaims();
        authenticated = true;
    }

    public DecodedJWT getJwt() {
        return jwt;
    }

    public Map<String, Claim> getClaims() {
        return claims;
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
