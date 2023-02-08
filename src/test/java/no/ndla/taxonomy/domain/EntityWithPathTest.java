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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EntityWithPathTest {
    private EntityWithPath entityWithPath;

    @BeforeEach
    public void setUp() {
        this.entityWithPath = spy(EntityWithPath.class);
    }

    @Test
    public void getCachedPaths() {
        final var cachedUrlSet = new HashSet<CachedPath>();

        when(entityWithPath.getCachedPaths()).thenReturn(cachedUrlSet);

        assertEquals(cachedUrlSet, entityWithPath.getCachedPaths());
    }

    @Test
    public void getPrimaryPath() {
        final var cachedUrl1 = mock(CachedPath.class);
        final var cachedUrl2 = mock(CachedPath.class);
        final var cachedUrl3 = mock(CachedPath.class);

        when(cachedUrl1.getPath()).thenReturn("/path1");
        when(cachedUrl2.getPath()).thenReturn("/path2");
        when(cachedUrl3.getPath()).thenReturn("/path3");
        when(cachedUrl1.isPrimary()).thenReturn(false);
        when(cachedUrl2.isPrimary()).thenReturn(false);
        when(cachedUrl3.isPrimary()).thenReturn(true);

        when(entityWithPath.getCachedPaths()).thenReturn(Set.of(cachedUrl1, cachedUrl2));
        assertFalse(entityWithPath.getPrimaryPath().isPresent());
        when(entityWithPath.getCachedPaths()).thenReturn(Set.of(cachedUrl1, cachedUrl2, cachedUrl3));
        assertTrue(entityWithPath.getPrimaryPath().isPresent());
        assertEquals("/path3", entityWithPath.getPrimaryPath().get());
    }

    @Test
    public void getPathByContext() throws URISyntaxException {
        final var cachedUrl1 = mock(CachedPath.class);
        final var cachedUrl2 = mock(CachedPath.class);
        final var cachedUrl3 = mock(CachedPath.class);
        final var cachedUrl31 = mock(CachedPath.class);
        final var cachedUrl4 = mock(CachedPath.class);

        when(cachedUrl1.getPath()).thenReturn("/context1/path1");
        when(cachedUrl2.getPath()).thenReturn("/context2/path1");
        when(cachedUrl3.getPath()).thenReturn("/context3/path1");
        when(cachedUrl31.getPath()).thenReturn("/context3/path2");
        when(cachedUrl4.getPath()).thenReturn("/context4/path1");
        when(cachedUrl1.isPrimary()).thenReturn(false);
        when(cachedUrl2.isPrimary()).thenReturn(true);
        when(cachedUrl3.isPrimary()).thenReturn(true);
        when(cachedUrl31.isPrimary()).thenReturn(false);
        when(cachedUrl4.isPrimary()).thenReturn(false);

        final var context1 = mock(DomainEntity.class);
        final var context2 = mock(DomainEntity.class);
        final var context3 = mock(DomainEntity.class);
        final var context4 = mock(DomainEntity.class);
        final var context5 = mock(DomainEntity.class);

        when(context1.getPublicId()).thenReturn(new URI("urn:context1"));
        when(context2.getPublicId()).thenReturn(new URI("urn:context2"));
        when(context3.getPublicId()).thenReturn(new URI("urn:context3"));
        when(context4.getPublicId()).thenReturn(new URI("urn:context4"));
        when(context5.getPublicId()).thenReturn(new URI("urn:context5"));

        when(entityWithPath.getCachedPaths())
                .thenReturn(Set.of(cachedUrl1, cachedUrl2, cachedUrl31, cachedUrl3, cachedUrl4));

        assertEquals("/context1/path1", entityWithPath.getPathByContext(context1).get());
        assertEquals("/context2/path1", entityWithPath.getPathByContext(context2).get());
        assertEquals("/context3/path1", entityWithPath.getPathByContext(context3).get());
        assertEquals("/context4/path1", entityWithPath.getPathByContext(context4).get());
        assertTrue(
                Set.of("/context2/path1", "/context3/path1").contains(entityWithPath.getPathByContext(context5).get()));
    }

    @Test
    public void getAllPaths() {
        final var cachedUrl1 = mock(CachedPath.class);
        final var cachedUrl2 = mock(CachedPath.class);
        final var cachedUrl3 = mock(CachedPath.class);

        when(cachedUrl1.getPath()).thenReturn("/path1");
        when(cachedUrl2.getPath()).thenReturn("/path2");
        when(cachedUrl3.getPath()).thenReturn("/path3");
        when(cachedUrl1.isPrimary()).thenReturn(false);
        when(cachedUrl2.isPrimary()).thenReturn(false);
        when(cachedUrl3.isPrimary()).thenReturn(true);

        when(entityWithPath.getCachedPaths()).thenReturn(Set.of(cachedUrl1, cachedUrl2, cachedUrl3));

        final var allPaths = entityWithPath.getAllPaths();

        assertEquals(3, allPaths.size());
        assertTrue(allPaths.contains("/path1"));
        assertTrue(allPaths.contains("/path2"));
        assertTrue(allPaths.contains("/path3"));
    }

    @Test
    public void addCachedPath() {
        final var cachedPathSet = new HashSet<CachedPath>();
        when(entityWithPath.getCachedPaths()).thenReturn(cachedPathSet);

        final var cachedPath1 = mock(CachedPath.class);
        entityWithPath.addCachedPath(cachedPath1);

        assertTrue(cachedPathSet.contains(cachedPath1));
        verify(cachedPath1).setOwningEntity(entityWithPath);

        final var cachedPath2 = mock(CachedPath.class);
        when(cachedPath2.getOwningEntity()).thenReturn(Optional.of(entityWithPath));
        entityWithPath.addCachedPath(cachedPath2);
        verify(cachedPath2, times(0)).setOwningEntity(any());

        assertTrue(cachedPathSet.containsAll(Set.of(cachedPath1, cachedPath2)));
    }

    @Test
    public void removeCachedPath() {
        final var cachedPath1 = mock(CachedPath.class);
        final var cachedPath2 = mock(CachedPath.class);
        when(cachedPath1.getOwningEntity()).thenReturn(Optional.of(entityWithPath));
        final var cachedPathSet = new HashSet<>(Set.of(cachedPath1, cachedPath2));

        when(entityWithPath.getCachedPaths()).thenReturn(cachedPathSet);

        entityWithPath.removeCachedPath(cachedPath1);
        verify(cachedPath1).setOwningEntity(null);
        assertEquals(1, cachedPathSet.size());
        assertTrue(cachedPathSet.contains(cachedPath2));

        entityWithPath.removeCachedPath(cachedPath2);
        verify(cachedPath2, times(0)).setOwningEntity(any());

        assertEquals(0, cachedPathSet.size());
    }
}
