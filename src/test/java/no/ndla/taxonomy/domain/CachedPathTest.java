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
        final var subject = mock(Topic.class);
        when(subject.getPublicId()).thenReturn(URI.create("urn:subject:1"));
        final var topic = mock(Topic.class);
        when(topic.getPublicId()).thenReturn(URI.create("urn:topic:1"));
        final var resource = mock(Resource.class);
        when(resource.getPublicId()).thenReturn(URI.create("urn:resource:1"));
        final var unknown = mock(EntityWithPath.class);

        assertNull(getField(cachedPath, "topic"));
        assertNull(getField(cachedPath, "resource"));

        cachedPath.setOwningEntity(subject);

        assertSame(subject, getField(cachedPath, "topic"));
        assertNull(getField(cachedPath, "resource"));
        assertEquals("urn:subject:1", cachedPath.getPublicId().toString());

        cachedPath.setOwningEntity(topic);

        assertSame(topic, getField(cachedPath, "topic"));
        assertNull(getField(cachedPath, "resource"));
        assertEquals("urn:topic:1", cachedPath.getPublicId().toString());

        cachedPath.setOwningEntity(resource);

        assertNull(getField(cachedPath, "topic"));
        assertSame(resource, getField(cachedPath, "resource"));
        assertEquals("urn:resource:1", cachedPath.getPublicId().toString());

        try {
            cachedPath.setOwningEntity(unknown);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        cachedPath.setOwningEntity(null);

        assertNull(getField(cachedPath, "topic"));
        assertNull(getField(cachedPath, "resource"));

        cachedPath.setOwningEntity(subject);
        cachedPath.setOwningEntity(null);

        assertNull(getField(cachedPath, "topic"));
        assertNull(getField(cachedPath, "resource"));

        cachedPath.setOwningEntity(topic);
        cachedPath.setOwningEntity(null);

        assertNull(getField(cachedPath, "topic"));
        assertNull(getField(cachedPath, "resource"));
    }

    @Test
    public void getOwningEntity() {
        assertFalse(cachedPath.getOwningEntity().isPresent());

        final var subject = mock(Topic.class);
        when(subject.getPublicId()).thenReturn(URI.create("urn:subject:1"));
        setField(cachedPath, "topic", subject);
        assertSame(subject, cachedPath.getOwningEntity().orElseThrow());

        final var topic = mock(Topic.class);
        when(topic.getPublicId()).thenReturn(URI.create("urn:topic:1"));

        setField(cachedPath, "topic", topic);

        assertSame(topic, cachedPath.getOwningEntity().orElseThrow());

        final var resource = mock(Resource.class);

        setField(cachedPath, "resource", resource);

        try {
            cachedPath.getOwningEntity();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ignored) {
        }

        setField(cachedPath, "topic", null);

        assertSame(resource, cachedPath.getOwningEntity().orElseThrow());
    }


    @Test
    public void getTopic() {
        assertFalse(cachedPath.getTopic().isPresent());

        final var topic = mock(Topic.class);
        when(topic.getPublicId()).thenReturn(URI.create("urn:topic:1"));

        setField(cachedPath, "topic", topic);

        assertSame(topic, cachedPath.getTopic().orElseThrow());
    }

    @Test
    public void getSubject() {
        assertFalse(cachedPath.getSubject().isPresent());

        final var subject = mock(Topic.class);
        when(subject.getPublicId()).thenReturn(URI.create("urn:subject:1"));

        setField(cachedPath, "topic", subject);

        assertSame(subject, cachedPath.getSubject().orElseThrow());
    }

    @Test
    public void getResource() {
        assertFalse(cachedPath.getResource().isPresent());

        final var resource = mock(Resource.class);

        setField(cachedPath, "resource", resource);

        assertSame(resource, cachedPath.getResource().orElseThrow());
    }

    @Test
    public void setSubject() {
        final var subject1 = mock(Topic.class);
        when(subject1.getPublicId()).thenReturn(URI.create("urn:subject:1"));
        final var subject2 = mock(Topic.class);
        when(subject2.getPublicId()).thenReturn(URI.create("urn:subject:2"));

        final var subject1CachedPaths = new HashSet<CachedPath>();
        final var subject2CachedPaths = new HashSet<CachedPath>();

        when(subject1.getCachedPaths()).thenReturn(subject1CachedPaths);
        when(subject2.getCachedPaths()).thenReturn(subject2CachedPaths);

        cachedPath.setSubject(subject1);
        verify(subject1, atLeastOnce()).addCachedPath(cachedPath);

        subject1CachedPaths.add(cachedPath);
        verify(subject1, times(0)).removeCachedPath(cachedPath);

        cachedPath.setSubject(subject2);

        verify(subject2).addCachedPath(cachedPath);
        verify(subject1).removeCachedPath(cachedPath);
    }

    @Test
    public void setTopic() {
        assertNull(getField(cachedPath, "topic"));

        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);

        final var topic1CachedPaths = new HashSet<CachedPath>();
        final var topic2CachedPaths = new HashSet<CachedPath>();

        when(topic1.getCachedPaths()).thenReturn(topic1CachedPaths);
        when(topic2.getCachedPaths()).thenReturn(topic2CachedPaths);

        cachedPath.setTopic(topic1);
        verify(topic1, atLeastOnce()).addCachedPath(cachedPath);

        topic1CachedPaths.add(cachedPath);
        verify(topic1, times(0)).removeCachedPath(cachedPath);

        cachedPath.setTopic(topic2);

        verify(topic2).addCachedPath(cachedPath);
        verify(topic1).removeCachedPath(cachedPath);
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