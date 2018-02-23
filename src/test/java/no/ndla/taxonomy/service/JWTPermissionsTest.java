package no.ndla.taxonomy.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JWTPermissionsTest {

    @Test
    public void can_parse_one_permission() {
        String scope = "drafts-staging:write";
        final JWTPermission jwtPermission = new JWTPermission(scope);
        assertEquals("drafts", jwtPermission.getApi());
        assertEquals("staging", jwtPermission.getEnvironment());
        assertEquals("write", jwtPermission.getPermission());
    }
}
