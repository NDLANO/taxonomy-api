/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NodeTest {
    private Node node;

    @BeforeEach
    public void setUp() {
        node = new Node(NodeType.NODE);
    }

    @Test
    public void name() {
        assertEquals(node, node.name("test name 1"));
        assertEquals("test name 1", node.getName());
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
        assertTrue(node.getParentNodes().isEmpty());

        final var connection1 = mock(NodeConnection.class);
        final var connection2 = mock(NodeConnection.class);

        try {
            node.addParentConnection(connection1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(connection1.getChild()).thenReturn(Optional.of(node));
        node.addParentConnection(connection1);

        assertFalse(node.getParentConnections().isEmpty());
        assertSame(connection1, node.getParentConnections().stream().findFirst().orElseThrow());

        try {
            node.addParentConnection(connection2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        node.removeParentConnection(connection1);
        verify(connection1).disassociate();
        assertTrue(node.getParentConnections().isEmpty());
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

        assertTrue(node.getParentNodes().isEmpty());

        node.addParentConnection(nodeConnection);

        assertFalse(node.getParentNodes().isEmpty());
        assertSame(parent1, node.getParentNodes().stream().findFirst().orElseThrow());
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
        assertEquals(3, node.getChildConnections().size());
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

        var returnedTranslation = node.addTranslation("hei", "nb");
        assertEquals(1, node.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(node.getTranslations().contains(returnedTranslation));
        assertEquals(node, returnedTranslation.getParent());

        var returnedTranslation2 = node.addTranslation("hello", "en");
        assertEquals(2, node.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(node.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(node, returnedTranslation2.getParent());

        node.removeTranslation("nb");

        assertNull(returnedTranslation.getParent());
        assertFalse(node.getTranslations().contains(returnedTranslation));

        assertFalse(node.getTranslation("nb").isPresent());

        node.addTranslation(returnedTranslation);
        assertEquals(node, returnedTranslation.getParent());
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
    void pickTheCorrectContext() {
        Node root = new Node(NodeType.NODE);
        root = root.name("root");
        Node parent1 = new Node(NodeType.NODE);
        parent1 = parent1.name("parent1");
        Node parent2 = new Node(NodeType.NODE);
        parent2 = parent2.name("parent2");
        node = node.name("name");
        var context1 = new Context(
                node.getPublicId().toString(),
                LanguageField.fromNode(node),
                node.getPathPart(),
                LanguageField.listFromNode(node),
                Optional.empty(),
                List.of(),
                true,
                true,
                true,
                "urn:relevance:core",
                "1",
                0,
                Optional.of("/root/f/1"),
                "urn:connection1");
        var context2 = new Context(
                root.getPublicId().toString(),
                LanguageField.fromNode(root),
                root.getPathPart() + parent1.getPathPart() + context1.path(),
                LanguageField.listFromLists(
                        LanguageField.listFromLists(LanguageField.listFromNode(root), LanguageField.fromNode(parent1)),
                        LanguageField.fromNode(node)),
                Optional.empty(),
                List.of(root.getPublicId().toString(), parent1.getPublicId().toString()),
                true,
                true,
                true,
                "urn:relevance:core",
                "2",
                0,
                Optional.of("/root/parent1/e/2"),
                "urn:connection2");
        var context3 = new Context(
                root.getPublicId().toString(),
                LanguageField.fromNode(root),
                root.getPathPart() + parent2.getPathPart() + context1.path(),
                LanguageField.listFromLists(
                        LanguageField.listFromLists(LanguageField.listFromNode(root), LanguageField.fromNode(parent2)),
                        LanguageField.fromNode(node)),
                Optional.empty(),
                List.of(root.getPublicId().toString(), parent2.getPublicId().toString()),
                true,
                true,
                true,
                "urn:relevance:core",
                "3",
                0,
                Optional.of("/root/parent2/e/3"),
                "urn:connection3");

        node.setContexts(Set.of(context3, context2, context1));

        {
            Optional<Context> context = node.pickContext(Optional.of("1"), Optional.empty(), Optional.empty());
            assertEquals(context1.contextId(), context.get().contextId());
        }
        {
            Optional<Context> context = node.pickContext(Optional.of("2"), Optional.empty(), Optional.empty());
            assertEquals(context2.contextId(), context.get().contextId());
        }
        {
            Optional<Context> context = node.pickContext(Optional.of("3"), Optional.empty(), Optional.empty());
            assertEquals(context3.contextId(), context.get().contextId());
        }
        {
            Optional<Context> context = node.pickContext(Optional.empty(), Optional.empty(), Optional.of(node));
            assertEquals(context1.contextId(), context.get().contextId());
        }
        {
            Optional<Context> context = node.pickContext(Optional.empty(), Optional.of(parent1), Optional.of(root));
            assertEquals(context2.contextId(), context.get().contextId());
        }
        {
            Optional<Context> context = node.pickContext(Optional.empty(), Optional.of(parent2), Optional.of(root));
            assertEquals(context3.contextId(), context.get().contextId());
        }
        {
            Optional<Context> context = node.pickContext(Optional.empty(), Optional.empty(), Optional.of(root));
            // could be either 2 or 3, either way root is root
            assertEquals(root.getPublicId().toString(), context.get().rootId());
        }
        {
            Optional<Context> context = node.pickContext(Optional.empty(), Optional.empty(), Optional.empty());
            assertEquals(context1.contextId(), context.get().contextId()); // Since context1 is shortest
        }
    }
}
