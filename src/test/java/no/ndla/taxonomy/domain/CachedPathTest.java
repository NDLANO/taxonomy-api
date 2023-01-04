/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class CachedPathTest {
    private CachedPath cachedPath;

    @BeforeEach
    public void setUp() {
        this.cachedPath = new CachedPath();
    }

    @Test
    public void setAndGetPublicId() throws URISyntaxException {
        assertNull(cachedPath.getPublicId());

        cachedPath.setPublicId(new URI("urn:test:1"));
        assertEquals(new URI("urn:test:1"), cachedPath.getPublicId());
    }

    @Test
    public void setAndGetPath() {
        assertNull(cachedPath.getPath());

        cachedPath.setPath("/test1/test2");
        assertEquals("/test1/test2", cachedPath.getPath());
    }

    @Test
    public void isPrimary() {
        assertFalse(cachedPath.isPrimary());
        cachedPath.setPrimary(true);
        assertTrue(cachedPath.isPrimary());
        cachedPath.setPrimary(false);
        assertFalse(cachedPath.isPrimary());
    }

    @Test
    public void prePersist() {
        assertNull(cachedPath.getId());

        cachedPath.prePersist();

        assertNotNull(cachedPath.getId());

        final var firstId = cachedPath.getId();

        cachedPath.prePersist();
        assertSame(firstId, cachedPath.getId());
    }

    @Test
    public void setOwningEntity() {
        final var subject = mock(Node.class);
        when(subject.getPublicId()).thenReturn(URI.create("urn:subject:1"));
        final var topic = mock(Node.class);
        when(topic.getPublicId()).thenReturn(URI.create("urn:topic:1"));
        final var resource = mock(Resource.class);
        when(resource.getPublicId()).thenReturn(URI.create("urn:resource:1"));
        final var unknown = mock(EntityWithPath.class);

        assertNull(getField(cachedPath, "node"));
        assertNull(getField(cachedPath, "resource"));

        cachedPath.setOwningEntity(subject);

        assertSame(subject, getField(cachedPath, "node"));
        assertNull(getField(cachedPath, "resource"));
        assertEquals("urn:subject:1", cachedPath.getPublicId().toString());

        cachedPath.setOwningEntity(topic);

        assertSame(topic, getField(cachedPath, "node"));
        assertNull(getField(cachedPath, "resource"));
        assertEquals("urn:topic:1", cachedPath.getPublicId().toString());

        cachedPath.setOwningEntity(resource);

        assertNull(getField(cachedPath, "node"));
        assertSame(resource, getField(cachedPath, "resource"));
        assertEquals("urn:resource:1", cachedPath.getPublicId().toString());

        try {
            cachedPath.setOwningEntity(unknown);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        cachedPath.setOwningEntity(null);

        assertNull(getField(cachedPath, "node"));
        assertNull(getField(cachedPath, "resource"));

        cachedPath.setOwningEntity(subject);
        cachedPath.setOwningEntity(null);

        assertNull(getField(cachedPath, "node"));
        assertNull(getField(cachedPath, "resource"));

        cachedPath.setOwningEntity(topic);
        cachedPath.setOwningEntity(null);

        assertNull(getField(cachedPath, "node"));
        assertNull(getField(cachedPath, "resource"));
    }

    @Test
    public void getOwningEntity() {
        assertFalse(cachedPath.getOwningEntity().isPresent());

        final var subject = mock(Node.class);
        setField(cachedPath, "node", subject);
        assertSame(subject, cachedPath.getOwningEntity().orElseThrow());

        final var topic = mock(Node.class);

        setField(cachedPath, "node", topic);

        try {
            cachedPath.getOwningEntity();
        } catch (IllegalStateException ignored) {
        }

        assertSame(topic, cachedPath.getOwningEntity().orElseThrow());

        final var resource = mock(Node.class);

        setField(cachedPath, "resource", resource);

        try {
            cachedPath.getOwningEntity();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ignored) {
        }

        setField(cachedPath, "node", null);

        assertSame(resource, cachedPath.getOwningEntity().orElseThrow());
    }
}
