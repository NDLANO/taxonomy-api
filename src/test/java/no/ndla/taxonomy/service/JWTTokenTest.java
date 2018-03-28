package no.ndla.taxonomy.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.Test;
import org.mockito.Mockito;

import static no.ndla.taxonomy.service.TestUtils.assertAnyTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class JWTTokenTest {

    String authorizationHeader = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoicjBnSGI5WGczbGk0eXlYdjBRU0dRY3pWM2J2aWFrclQiLCJpc3MiOiJodHRwczovL25kbGEuZXUuYXV0aDAuY29tLyIsInN1YiI6InIwZ0hiOVhnM2xpNHl5WHYwUVNHUWN6VjNidmlha3JUQGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxNTQ5NTUzMCwiZXhwIjoxNTE1NTgxOTMwLCJzY29wZSI6ImRyYWZ0cy1zdGFnaW5nOndyaXRlIGFydGljbGVzLXRlc3Q6cHVibGlzaCBpbWFnZXMtcHJvZDp3cml0ZSBsaXN0aW5nLXRlc3Q6d3JpdGUgYXJ0aWNsZXMtYnJ1a2VydGVzdDp3cml0ZSBsaXN0aW5nLXByb2Q6d3JpdGUgYXJ0aWNsZXMtdGVzdDp3cml0ZSBhcnRpY2xlcy1zdGFnaW5nOndyaXRlIGxpc3RpbmctYnJ1a2VydGVzdDp3cml0ZSBpbWFnZXMtc3RhZ2luZzp3cml0ZSBpbWFnZXMtdGVzdDp3cml0ZSBhcnRpY2xlcy1wcm9kOnB1Ymxpc2ggYXJ0aWNsZXMtYnJ1a2VydGVzdDpwdWJsaXNoIGRyYWZ0cy10ZXN0OnNldF90b19wdWJsaXNoIGxpc3Rpbmctc3RhZ2luZzp3cml0ZSBhdWRpby1icnVrZXJ0ZXN0OndyaXRlIGRyYWZ0cy1wcm9kOndyaXRlIGltYWdlcy1icnVrZXJ0ZXN0OndyaXRlIGRyYWZ0cy10ZXN0OndyaXRlIGF1ZGlvLXByb2Q6d3JpdGUgYXVkaW8tdGVzdDp3cml0ZSBhcnRpY2xlcy1zdGFnaW5nOnB1Ymxpc2ggYXJ0aWNsZXMtcHJvZDp3cml0ZSBkcmFmdHMtc3RhZ2luZzpzZXRfdG9fcHVibGlzaCBhdWRpby1zdGFnaW5nOndyaXRlIGRyYWZ0cy1icnVrZXJ0ZXN0OnNldF90b19wdWJsaXNoIGRyYWZ0cy1wcm9kOnNldF90b19wdWJsaXNoIGRyYWZ0cy1icnVrZXJ0ZXN0OndyaXRlIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.cOhrV1JshGYyD-CI-jh_omwy_ehDZJFq1P3C3FibhgcG2V3q6emPftmyOfKstp5_HxBWfULeq4p3Du5GlpJ5oADGSES2KlfSAZFQQWoN2auCqL4TP7lR7syosEftwWa6XIYMgzqk5r4KH4F0ay0v5n7_sU99r6HDDi8bsuLpgoXgcRwtx_SIh_CJWplS30Iiz8EdITdvknU40_t8zeqp7CoYnnZRQtm79qHxMGXyyiSqGPISUkzEK6O8HPNh0dVWvyI-SNzAQyJQg8J3RC26bgo7SpVDt8oGGMiKsBnssuXO60MqEs_vt3Q4-xdXIJQOj0YX4ICEeHJ3nq1pigWRLg";

    @Test
    public void no_taxonomy_in_jwt_token_gives_read_permission() {
        DecodedJWT jwt = JWT.decode(authorizationHeader.substring(6).trim());

        JWTToken token = new JWTToken(jwt);
        assertTrue(token.isAuthenticated());
        assertEquals(1, token.getAuthorities().size());
    }

    @Test
    public void write_gives_write_permission() {
        DecodedJWT jwt = JWT.decode(authorizationHeader.substring(6).trim());

        final DecodedJWT mockDecodedJWT = Mockito.mock(DecodedJWT.class);
        Claim claim = Mockito.mock(Claim.class);
        when(claim.asString()).thenReturn("taxonomy-prod:write drafts-staging:write articles-test:publish images-prod:write listing-test:write articles-brukertest:write listing-prod:write articles-test:write articles-staging:write listing-brukertest:write images-staging:write images-test:write articles-prod:publish articles-brukertest:publish drafts-test:set_to_publish listing-staging:write audio-brukertest:write drafts-prod:write images-brukertest:write drafts-test:write audio-prod:write audio-test:write articles-staging:publish articles-prod:write drafts-staging:set_to_publish audio-staging:write drafts-brukertest:set_to_publish drafts-prod:set_to_publish drafts-brukertest:write drafts-taxonomy:write ");
        when(mockDecodedJWT.getClaim("scope")).thenReturn(claim);
        JWTToken token = new JWTToken(mockDecodedJWT);
        assertEquals(2, token.getAuthorities().size());
        assertAnyTrue(token.getAuthorities(), au -> "TAXONOMY_WRITE".equals(au.getAuthority()));
    }
}
