/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.commands.VersionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Optional;
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

    @Autowired
    private VersionService versionService;

    @MockBean
    private TreeSorter treeSorter;

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
    }

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

    @Test
    public void allSearch() {
        builder.node(n -> n.nodeType(NodeType.TOPIC));
        builder.node(n -> n.nodeType(NodeType.TOPIC));
        builder.node(n -> n.nodeType(NodeType.TOPIC));
        builder.node(n -> n.nodeType(NodeType.TOPIC));
        var subject = builder.node(n -> n.nodeType(NodeType.SUBJECT));

        var subjects = nodeService.searchByNodeType(Optional.empty(), Optional.empty(), Optional.empty(), 10, 1,
                Optional.of(NodeType.SUBJECT));

        var topics = nodeService.searchByNodeType(Optional.empty(), Optional.empty(), Optional.empty(), 10, 1,
                Optional.of(NodeType.TOPIC));
        var all = nodeService.searchByNodeType(Optional.empty(), Optional.empty(), Optional.empty(), 10, 1,
                Optional.empty());

        assertEquals(subjects.getResults().get(0).getId(), subject.getPublicId());

        assertEquals(subjects.getTotalCount(), 1);
        assertEquals(topics.getTotalCount(), 4);
        assertEquals(all.getTotalCount(), 5);
    }

    @Test
    public void querySearchWorks() {
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Apekatt"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Katt"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Hund"));
        var tiger = builder.node(n -> n.nodeType(NodeType.TOPIC).name("Tiger"));

        var result = nodeService.search(Optional.of("tiger"), Optional.empty(), Optional.empty(), 10, 1);

        assertEquals(result.getResults().get(0).getId(), tiger.getPublicId());
        assertEquals(result.getTotalCount(), 1);
    }

    @Test
    public void idsAndQuerySearch() throws URISyntaxException {
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Apekatt").publicId("urn:topic:1"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Katt").publicId("urn:topic:2"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Hund").publicId("urn:topic:3"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Tiger").publicId("urn:topic:4"));

        var idList = new ArrayList<String>();
        idList.add("urn:topic:1");
        idList.add("urn:topic:2");

        var result = nodeService.search(Optional.empty(), Optional.of(idList), Optional.empty(), 10, 1);

        assertEquals(result.getResults().get(0).getId(), new URI("urn:topic:1"));
        assertEquals(result.getResults().get(1).getId(), new URI("urn:topic:2"));
        assertEquals(result.getTotalCount(), 2);

        var result2 = nodeService.search(Optional.of("Ape"), Optional.of(idList), Optional.empty(), 10, 1);

        assertEquals(result2.getResults().get(0).getId(), new URI("urn:topic:1"));
        assertEquals(result2.getTotalCount(), 1);
    }

}
