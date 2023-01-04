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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class NodeTest {
    private Node node;

    @BeforeEach
    public void setUp() {
        node = new Node();
    }

    @Test
    public void name() {
        assertEquals(node, node.name("test name 1"));
        assertEquals("test name 1", node.getName());
    }

    @Test
    public void getPrimaryPath() {
        assertFalse(node.getPrimaryPath().isPresent());

        final var cachedPrimaryContextUrl = mock(CachedPath.class);
        final var cachedPrimarySubjectUrl = mock(CachedPath.class);

        when(cachedPrimaryContextUrl.isActive()).thenReturn(true);
        when(cachedPrimaryContextUrl.isPrimary()).thenReturn(true);
        when(cachedPrimaryContextUrl.getPath()).thenReturn("/topic/primary");
        when(cachedPrimarySubjectUrl.isActive()).thenReturn(true);
        when(cachedPrimarySubjectUrl.isPrimary()).thenReturn(true);
        when(cachedPrimarySubjectUrl.getPath()).thenReturn("/subject/primary");

        // If returning a primary subject path along it must return it
        setField(node, "cachedPaths", Set.of(cachedPrimarySubjectUrl));
        assertEquals("/subject/primary", node.getPrimaryPath().orElse(""));

        // And adding a primary context URL (topic) it must be returned instead
        setField(node, "cachedPaths", Set.of(cachedPrimaryContextUrl));
        assertEquals("/topic/primary", node.getPrimaryPath().orElse(""));

        // Order must not matter
        // setField(node, "cachedPaths", Set.of(cachedPrimaryContextUrl, cachedPrimarySubjectUrl));
        // assertEquals("/topic/primary", node.getPrimaryPath().orElse(""));
    }

    @Test
    public void getAddAndRemoveSubjectTopics() {
        final var node = spy(this.node);

        assertEquals(0, node.getParentConnections().size());

        final var connection1 = mock(NodeConnection.class);
        final var connection2 = mock(NodeConnection.class);

        try {
            node.addParentConnection(connection1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }

        when(connection1.getChild()).thenReturn(Optional.of(node));
        node.addParentConnection(connection1);

        when(connection1.getParent()).thenReturn(Optional.of(node));

        assertEquals(1, node.getParentConnections().size());
        assertTrue(node.getParentConnections().contains(connection1));

        try {
            node.addParentConnection(connection2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(connection2.getChild()).thenReturn(Optional.of(node));
        node.addParentConnection(connection2);

        when(connection2.getParent()).thenReturn(Optional.of(node));

        assertEquals(2, node.getParentConnections().size());
        assertTrue(node.getParentConnections().containsAll(Set.of(connection1, connection2)));

        node.removeParentConnection(connection1);
        assertEquals(1, node.getParentConnections().size());
        assertTrue(node.getParentConnections().contains(connection2));
        verify(connection1).disassociate();

        node.removeParentConnection(connection2);
        assertEquals(0, node.getParentConnections().size());
        verify(connection2).disassociate();
    }

    @Test
    public void addGetAndRemoveChildrenTopicSubtopics() {
        assertEquals(0, node.getChildConnections().size());

        final var connection1 = mock(NodeConnection.class);
        final var connection2 = mock(NodeConnection.class);

        try {
            node.addChildConnection(connection1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }
        when(connection1.getParent()).thenReturn(Optional.of(node));
        node.addChildConnection(connection1);

        assertEquals(1, node.getChildConnections().size());
        assertTrue(node.getChildConnections().contains(connection1));

        try {
            node.addChildConnection(connection2);
        } catch (IllegalArgumentException ignored) {
        }
        when(connection2.getParent()).thenReturn(Optional.of(node));
        node.addChildConnection(connection2);

        assertEquals(2, node.getChildConnections().size());
        assertTrue(node.getChildConnections().containsAll(Set.of(connection1, connection2)));

        node.removeChildConnection(connection1);
        verify(connection1).disassociate();
        assertEquals(1, node.getChildConnections().size());
        assertTrue(node.getChildConnections().contains(connection2));

        node.removeChildConnection(connection2);
        verify(connection2).disassociate();
        assertEquals(0, node.getChildConnections().size());
    }

    @Test
    public void addGetAndRemoveParentTopicSubtopics() {
        assertFalse(node.getParentNode().isPresent());

        final var connection1 = mock(NodeConnection.class);
        final var connection2 = mock(NodeConnection.class);

        try {
            node.addParentConnection(connection1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(connection1.getChild()).thenReturn(Optional.of(node));
        node.addParentConnection(connection1);

        assertTrue(node.getParentConnection().isPresent());
        assertSame(connection1, node.getParentConnection().orElseThrow());

        try {
            node.addParentConnection(connection2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        node.removeParentConnection(connection1);
        verify(connection1).disassociate();
        assertFalse(node.getParentConnection().isPresent());
    }

    @Test
    public void addGetAndRemoveNodeResources() {
        assertEquals(0, node.getResourceChildren().size());

        final var nodeResource1 = mock(NodeConnection.class);
        final var nodeResource2 = mock(NodeConnection.class);
        final var resource1 = mock(Node.class);
        final var resource2 = mock(Node.class);
        when(resource1.getNodeType()).thenReturn(NodeType.RESOURCE);
        when(resource2.getNodeType()).thenReturn(NodeType.RESOURCE);
        when(nodeResource1.getChild()).thenReturn(Optional.of(resource1));
        when(nodeResource2.getChild()).thenReturn(Optional.of(resource2));

        try {
            node.addChildConnection(nodeResource1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(nodeResource1.getParent()).thenReturn(Optional.of(node));
        node.addChildConnection(nodeResource1);

        assertEquals(1, node.getResourceChildren().size());
        assertTrue(node.getResourceChildren().contains(nodeResource1));

        try {
            node.addChildConnection(nodeResource2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(nodeResource2.getParent()).thenReturn(Optional.of(node));
        node.addChildConnection(nodeResource2);

        assertEquals(2, node.getResourceChildren().size());
        assertTrue(node.getResourceChildren().containsAll(Set.of(nodeResource1, nodeResource2)));

        node.removeChildConnection(nodeResource1);
        verify(nodeResource1).disassociate();
        assertEquals(1, node.getResourceChildren().size());
        assertTrue(node.getResourceChildren().contains(nodeResource2));

        node.removeChildConnection(nodeResource2);
        verify(nodeResource2).disassociate();
        assertEquals(0, node.getResourceChildren().size());
    }

    @Test
    public void getParent() {
        final var parent1 = mock(Node.class);

        final var nodeConnection = mock(NodeConnection.class);

        when(nodeConnection.getChild()).thenReturn(Optional.of(node));
        when(nodeConnection.getParent()).thenReturn(Optional.of(parent1));

        assertFalse(node.getParentNode().isPresent());

        node.addParentConnection(nodeConnection);

        assertTrue(node.getParentNode().isPresent());
        assertSame(parent1, node.getParentNode().orElseThrow());
    }

    @Test
    public void getResources() {
        final var resource1 = mock(Node.class);
        final var resource2 = mock(Node.class);

        final var nodeResource1 = mock(NodeConnection.class);
        final var nodeResource2 = mock(NodeConnection.class);
        final var NodeResource3 = mock(NodeConnection.class);

        Set.of(nodeResource1, nodeResource2, NodeResource3)
                .forEach(nodeResource -> when(nodeResource.getParent()).thenReturn(Optional.of(node)));
        when(resource1.getNodeType()).thenReturn(NodeType.RESOURCE);
        when(resource2.getNodeType()).thenReturn(NodeType.RESOURCE);
        when(nodeResource1.getResource()).thenReturn(Optional.of(resource1));
        when(nodeResource2.getResource()).thenReturn(Optional.of(resource2));
        when(nodeResource1.getChild()).thenReturn(Optional.of(resource1));
        when(nodeResource2.getChild()).thenReturn(Optional.of(resource2));

        node.addChildConnection(nodeResource1);
        node.addChildConnection(nodeResource2);
        node.addChildConnection(NodeResource3);

        assertEquals(2, node.getResourceChildren().size());
        assertEquals(3, node.getChildren().size());
        assertEquals(2, node.getResources().size());
        assertTrue(node.getResources().containsAll(Set.of(resource1, resource2)));
    }

    @Test
    public void setAndGetContentUri() throws URISyntaxException {
        assertNull(node.getContentUri());
        node.setContentUri(new URI("urn:test1"));
        assertEquals("urn:test1", node.getContentUri().toString());
    }

    @Test
    public void setAndIsContext() {
        assertFalse(node.isContext());
        node.setContext(true);
        assertTrue(node.isContext());
        node.setContext(false);
        assertFalse(node.isContext());
    }

    @Test
    public void addAndGetAndRemoveTranslation() {
        assertEquals(0, node.getTranslations().size());

        var returnedTranslation = node.addTranslation("nb");
        assertEquals(1, node.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(node.getTranslations().contains(returnedTranslation));
        assertEquals(node, returnedTranslation.getNode());

        var returnedTranslation2 = node.addTranslation("en");
        assertEquals(2, node.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(node.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(node, returnedTranslation2.getNode());

        node.removeTranslation("nb");

        assertNull(returnedTranslation.getNode());
        assertFalse(node.getTranslations().contains(returnedTranslation));

        assertFalse(node.getTranslation("nb").isPresent());

        node.addTranslation(returnedTranslation);
        assertEquals(node, returnedTranslation.getNode());
        assertTrue(node.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, node.getTranslation("nb").get());
        assertEquals(returnedTranslation2, node.getTranslation("en").orElse(null));
    }

    @Test
    public void preRemove() {
        final var parentConnections = Set.of(mock(NodeConnection.class), mock(NodeConnection.class));
        final var childConnections = Set.of(mock(NodeConnection.class), mock(NodeConnection.class));
        final var topicResources = Set.of(mock(NodeConnection.class), mock(NodeConnection.class));

        parentConnections.forEach(nodeConnection -> {
            when(nodeConnection.getChild()).thenReturn(Optional.of(node));
            node.addParentConnection(nodeConnection);
        });

        childConnections.forEach(nodeConnection -> {
            when(nodeConnection.getParent()).thenReturn(Optional.of(node));
            node.addChildConnection(nodeConnection);
        });

        topicResources.forEach(nodeResource -> {
            when(nodeResource.getParent()).thenReturn(Optional.of(node));
            node.addChildConnection(nodeResource);
        });

        node.preRemove();

        parentConnections.forEach(nodeConnection -> verify(nodeConnection).disassociate());
        childConnections.forEach(nodeConnection -> verify(nodeConnection).disassociate());
        topicResources.forEach(nodeResource -> verify(nodeResource).disassociate());
    }

    @Test
    public void getCachedPaths() {
        final var cachedPaths = Set.of();

        setField(node, "cachedPaths", cachedPaths);
        assertSame(cachedPaths, node.getCachedPaths());
    }
}
