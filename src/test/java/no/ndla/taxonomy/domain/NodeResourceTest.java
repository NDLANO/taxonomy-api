/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NodeResourceTest {
    private Node node;
    private Resource resource;
    private NodeResource resourceConnection;

    @BeforeEach
    public void setUp() {
        node = mock(Node.class);
        resource = mock(Resource.class);

        resourceConnection = NodeResource.create(node, resource);

        verify(node).addNodeResource(resourceConnection);
        verify(resource).addNodeResource(resourceConnection);

        assertNotNull(resourceConnection.getPublicId());
        assertTrue(resourceConnection.getPublicId().toString().length() > 4);
    }

    @Test
    public void getNode() {
        assertSame(node, resourceConnection.getNode().orElse(null));
    }

    @Test
    public void getResource() {
        assertSame(resource, resourceConnection.getResource().orElse(null));
    }

    @Test
    public void setAndIsPrimary() {
        assertFalse(resourceConnection.isPrimary().orElseThrow());
        resourceConnection.setPrimary(true);
        assertTrue(resourceConnection.isPrimary().orElseThrow());
        resourceConnection.setPrimary(false);
        assertFalse(resourceConnection.isPrimary().orElseThrow());
    }

    @Test
    public void setAndGetRank() {
        assertEquals(0, resourceConnection.getRank());
        resourceConnection.setRank(100);
        assertEquals(100, resourceConnection.getRank());
    }

    @Test
    public void preRemove() {
        resourceConnection.preRemove();

        assertFalse(resourceConnection.getResource().isPresent());
        assertFalse(resourceConnection.getNode().isPresent());

        verify(node).removeNodeResource(resourceConnection);
        verify(resource).removeNodeResource(resourceConnection);
    }
}
