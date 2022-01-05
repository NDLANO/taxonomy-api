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

        cachedPath.setOwningEntity(topic);

        assertSame(topic, getField(cachedPath, "node"));
        assertNull(getField(cachedPath, "resource"));

        cachedPath.setOwningEntity(resource);

        assertNull(getField(cachedPath, "node"));
        assertSame(resource, getField(cachedPath, "resource"));

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

        final var resource = mock(Resource.class);

        setField(cachedPath, "resource", resource);

        try {
            cachedPath.getOwningEntity();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ignored) {
        }

        setField(cachedPath, "node", null);

        assertSame(resource, cachedPath.getOwningEntity().orElseThrow());
    }

    @Test
    public void getResource() {
        assertFalse(cachedPath.getResource().isPresent());

        final var resource = mock(Resource.class);

        setField(cachedPath, "resource", resource);

        assertSame(resource, cachedPath.getResource().orElseThrow());
    }

    @Test
    public void setResource() {
        assertNull(getField(cachedPath, "resource"));

        final var resource1 = mock(Resource.class);
        final var resource2 = mock(Resource.class);

        final var resource1CachedPaths = new HashSet<CachedPath>();
        final var resource2CachedPaths = new HashSet<CachedPath>();

        when(resource1.getCachedPaths()).thenReturn(resource1CachedPaths);
        when(resource2.getCachedPaths()).thenReturn(resource2CachedPaths);

        cachedPath.setResource(resource1);
        verify(resource1, atLeastOnce()).addCachedPath(cachedPath);

        resource1CachedPaths.add(cachedPath);
        verify(resource1, times(0)).removeCachedPath(cachedPath);

        cachedPath.setResource(resource2);

        verify(resource2).addCachedPath(cachedPath);
        verify(resource1).removeCachedPath(cachedPath);
    }
}
