package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CachedUrlTest {
    private CachedUrl cachedUrl;

    @Before
    public void setUp() {
        this.cachedUrl = new CachedUrl();
    }

    @Test
    public void testConstructor() throws URISyntaxException {
        final var cachedUrl = new CachedUrl(new URI("urn:test1"), "/test/path");
        assertEquals("urn:test1", cachedUrl.getPublicId().toString());
        assertEquals("/test/path", cachedUrl.getPath());
    }

    @Test
    public void setAndGetPublicId() throws URISyntaxException {
        assertNull(cachedUrl.getPublicId());

        cachedUrl.setPublicId(new URI("urn:test:1"));
        assertEquals(new URI("urn:test:1"), cachedUrl.getPublicId());
    }

    @Test
    public void setAndGetPath() {
        assertNull(cachedUrl.getPath());

        cachedUrl.setPath("/test1/test2");
        assertEquals("/test1/test2", cachedUrl.getPath());
    }
}