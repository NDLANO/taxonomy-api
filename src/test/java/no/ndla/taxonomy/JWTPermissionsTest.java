package no.ndla.taxonomy;

import no.ndla.taxonomy.security.JWTPermission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JWTPermissionsTest {

    @Test
    public void can_parse_one_permission() {
        String scope = "drafts-staging:write";
        final JWTPermission jwtPermission = new JWTPermission(scope);
        assertEquals("drafts", jwtPermission.getApi());
        assertEquals("staging", jwtPermission.getEnvironment());
        assertEquals("write", jwtPermission.getPermission());
    }
    @Test
    public void can_parse_permission_with_no_dash() {
        String scope = "drafts:write";
        final JWTPermission jwtPermission = new JWTPermission(scope);
        assertEquals("drafts", jwtPermission.getApi());
        assertEquals("write", jwtPermission.getPermission());
        assertNull(jwtPermission.getEnvironment());
    }
}
