/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
public class NodeServiceTest {
    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private Builder builder;

    @MockBean
    private EntityConnectionService entityConnectionService;

    @Autowired
    private NodeService nodeService;

    @MockBean
    private TreeSorter treeSorter;

    @Test
    public void delete() {
        final var createdTopic = builder.node(n -> n.nodeType(NodeType.TOPIC));
        final var topicId = createdTopic.getPublicId();
        nodeService.delete(topicId);

        assertFalse(nodeRepository.findFirstByPublicId(topicId).isPresent());
        verify(entityConnectionService).disconnectAllChildren(createdTopic);
    }

    @Test
    public void getAllConnections() {
        final var topicId = builder.node(n -> n.nodeType(NodeType.TOPIC)).getPublicId();

        final var subjectTopic = mock(NodeConnection.class);
        final var parentTopicSubtopic = mock(NodeConnection.class);
        final var childTopicSubtopic = mock(NodeConnection.class);

        when(subjectTopic.getPublicId()).thenReturn(URI.create("urn:subject-topic"));
        when(parentTopicSubtopic.getPublicId()).thenReturn(URI.create("urn:parent-topic"));
        when(childTopicSubtopic.getPublicId()).thenReturn(URI.create("urn:child-topic"));

        final var parentConnectionsToReturn = Set.of(subjectTopic);
        final var childConnectionsToReturn = Set.of(childTopicSubtopic);

        when(entityConnectionService.getParentConnections(any(Node.class))).thenAnswer(invocationOnMock -> {
            final var topic = (Node) invocationOnMock.getArgument(0);
            assertEquals(topicId, topic.getPublicId());

            return parentConnectionsToReturn;
        });
        when(entityConnectionService.getChildConnections(any(Node.class))).thenAnswer(invocationOnMock -> {
            final var topic = (Node) invocationOnMock.getArgument(0);
            assertEquals(topicId, topic.getPublicId());

            return childConnectionsToReturn;
        });

        final var returnedConnections = nodeService.getAllConnections(topicId);

        assertEquals(2, returnedConnections.size());
        returnedConnections.forEach(connection -> {
            if (connection.getConnectionId().equals(URI.create("urn:subject-topic"))) {
                assertEquals("parent-topic", connection.getType());
            } else if (connection.getConnectionId().equals(URI.create("urn:child-topic"))) {
                assertEquals("subtopic", connection.getType());
            } else {
                fail();
            }
        });
    }

    static class MockedSortedArrayList<E> extends ArrayList<E> {
        private MockedSortedArrayList(Collection<E> collection) {
            super(collection);
        }
    }
}