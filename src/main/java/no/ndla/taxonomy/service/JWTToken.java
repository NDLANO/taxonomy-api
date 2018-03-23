package no.ndla.taxonomy.service;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

public class JWTToken implements Authentication {

    private static final long serialVersionUID = 1L;
    private static final String TAXONOMY_API = "taxonomy";
    private static final String WRITE_PERMISSION = "write";
    private static final String PRODUCTION = "prod";

    private DecodedJWT jwt;
    private Collection<GrantedAuthority> authorities;
    private boolean authenticated;
    private Map<String, Claim> claims;

    public JWTToken(DecodedJWT jwt) {

        this.jwt = jwt;

        Claim appMetadata = this.jwt.getClaim("scope");

        List<GrantedAuthority> tmp = new ArrayList<>();
        tmp.add(new SimpleGrantedAuthority("READONLY"));

        try {
            final String[] allPermissions = appMetadata.asString().split(" ");
            for (String jwtPermissionString : allPermissions) {
                final JWTPermission jwtPermission = new JWTPermission(jwtPermissionString);
                if (jwtPermission.getApi().equals(TAXONOMY_API) && jwtPermission.getPermission().equals(WRITE_PERMISSION) && jwtPermission.getEnvironment().equals(PRODUCTION)) {
                    tmp.add(new SimpleGrantedAuthority("TAXONOMY_WRITE"));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        this.authorities = Collections.unmodifiableList(tmp);
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
