package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class EntityWithPathTest {
    private EntityWithPath entityWithPath;

    @Before
    public void setUp() {
        this.entityWithPath = spy(EntityWithPath.class);
    }

    @Test
    public void getCachedUrls() {
        final var cachedUrlSet = new HashSet<>();

        setField(entityWithPath, "cachedUrls", cachedUrlSet);

        assertEquals(cachedUrlSet, entityWithPath.getCachedUrls());
    }

    @Test
    public void getPrimaryPath() {
        final var cachedUrl1 = mock(CachedUrl.class);

        when(cachedUrl1.getPath()).thenReturn("/path1");

        setField(entityWithPath, "cachedUrls", Set.of(cachedUrl1));
        assertEquals("/path1", entityWithPath.getPrimaryPath().orElseThrow());
    }

    @Test
    public void getPathByContext() throws URISyntaxException {
        final var cachedUrl2 = mock(CachedUrl.class);
        final var cachedUrl3 = mock(CachedUrl.class);

        when(cachedUrl2.getPath()).thenReturn("/context2/path1");
        when(cachedUrl3.getPath()).thenReturn("/context3/path1");

        final var context2 = mock(DomainEntity.class);
        final var context3 = mock(DomainEntity.class);
        final var context4 = mock(DomainEntity.class);
        final var context5 = mock(DomainEntity.class);

        when(context2.getPublicId()).thenReturn(new URI("urn:context2"));
        when(context3.getPublicId()).thenReturn(new URI("urn:context3"));
        when(context4.getPublicId()).thenReturn(new URI("urn:context4"));
        when(context5.getPublicId()).thenReturn(new URI("urn:context5"));


        setField(entityWithPath, "cachedUrls", Set.of(cachedUrl2, cachedUrl3));

        assertEquals("/context2/path1", entityWithPath.getPathByContext(context2).get());
        assertEquals("/context3/path1", entityWithPath.getPathByContext(context3).get());
        assertTrue(Set.of("/context2/path1", "/context3/path1").contains(entityWithPath.getPathByContext(context5).get()));
    }

    @Test
    public void getAllPaths() {
        final var cachedUrl1 = mock(CachedUrl.class);
        final var cachedUrl2 = mock(CachedUrl.class);
        final var cachedUrl3 = mock(CachedUrl.class);

        when(cachedUrl1.getPath()).thenReturn("/path1");
        when(cachedUrl2.getPath()).thenReturn("/path2");
        when(cachedUrl3.getPath()).thenReturn("/path3");

        setField(entityWithPath, "cachedUrls", Set.of(cachedUrl1, cachedUrl2, cachedUrl3));

        final var allPaths = entityWithPath.getAllPaths();

        assertEquals(3, allPaths.size());
        assertTrue(allPaths.contains("/path1"));
        assertTrue(allPaths.contains("/path2"));
        assertTrue(allPaths.contains("/path3"));
    }
}