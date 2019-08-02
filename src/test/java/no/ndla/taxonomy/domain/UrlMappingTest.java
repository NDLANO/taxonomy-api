package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class UrlMappingTest {
    private UrlMapping urlMapping;

    @Before
    public void setUp() {
        urlMapping = new UrlMapping();
    }

    @Test
    public void setAndGetOldUrl() {
        urlMapping.setOldUrl("urn:test-1");
        assertEquals("urn:test-1", urlMapping.getOldUrl());
    }

    @Test
    public void setAndGetPublic_id() throws URISyntaxException {
        urlMapping.setPublic_id(new URI("urn:test-public-1"));
        assertEquals("urn:test-public-1", urlMapping.getPublic_id().toString());

        urlMapping.setPublic_id("urn:test-public-2");
        assertEquals("urn:test-public-2", urlMapping.getPublic_id().toString());
    }

    @Test
    public void getAndSetSubject_id() throws URISyntaxException {
        urlMapping.setSubject_id(new URI("urn:test-subject-id-1"));
        assertEquals("urn:test-subject-id-1", urlMapping.getSubject_id().toString());

        urlMapping.setSubject_id("urn:test-subject-id-2");
        assertEquals("urn:test-subject-id-2", urlMapping.getSubject_id().toString());
    }
}