/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeResource;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeResourceRepository;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
public class EntityConnectionServiceImplTest {
    @Autowired
    private NodeConnectionRepository nodeConnectionRepository;
    @Autowired
    private NodeResourceRepository nodeResourceRepository;

    private CachedUrlUpdaterService cachedUrlUpdaterService;

    private EntityConnectionServiceImpl service;

    @Autowired
    private Builder builder;

    @BeforeEach
    public void setUp() throws Exception {
        cachedUrlUpdaterService = mock(CachedUrlUpdaterService.class);

        service = new EntityConnectionServiceImpl(nodeConnectionRepository, nodeResourceRepository,
                cachedUrlUpdaterService);
    }

    @Test
    public void connectTopicSubtopic() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);
        final var topic4 = builder.node(NodeType.TOPIC);
        final var topic5 = builder.node(NodeType.TOPIC);
        final var topic6 = builder.node(NodeType.TOPIC);
        final var topic7 = builder.node(NodeType.TOPIC);
        final var topic8 = builder.node(NodeType.TOPIC);
        final var topic9 = builder.node(NodeType.TOPIC);

        final var relevance = builder.relevance();

        final var connection1 = service.connectParentChild(topic2, topic1, null, 1);
        assertNotNull(connection1);
        assertNotNull(connection1.getId());
        assertEquals(1, connection1.getRank());
        assertSame(topic2, connection1.getParent().orElse(null));
        assertSame(topic1, connection1.getChild().orElse(null));

        verify(cachedUrlUpdaterService, atLeastOnce()).updateCachedUrls(topic1);

        // Test ranking
        final var connection4 = service.connectParentChild(topic4, topic5, null, null);
        assertEquals(1, connection4.getRank());

        final var connection5 = service.connectParentChild(topic4, topic6, null, null);
        assertEquals(1, connection4.getRank());
        assertEquals(2, connection5.getRank());

        final var connection6 = service.connectParentChild(topic4, topic7, relevance, 1);
        assertEquals(2, connection4.getRank());
        assertEquals(3, connection5.getRank());
        assertEquals(1, connection6.getRank());

        final var connection7 = service.connectParentChild(topic4, topic8, relevance, 3);
        assertEquals(2, connection4.getRank());
        assertEquals(4, connection5.getRank());
        assertEquals(1, connection6.getRank());
        assertEquals(3, connection7.getRank());

        final var connection8 = service.connectParentChild(topic4, topic9, relevance, 5);
        assertEquals(2, connection4.getRank());
        assertEquals(4, connection5.getRank());
        assertEquals(1, connection6.getRank());
        assertEquals(3, connection7.getRank());
        assertEquals(5, connection8.getRank());

        try {
            service.connectParentChild(topic4, topic8, relevance, 0);
        } catch (DuplicateConnectionException ignored) {

        }

        // try to create loops
        final var topic10 = builder.node(NodeType.TOPIC);
        final var topic11 = builder.node(NodeType.TOPIC);
        final var topic12 = builder.node(NodeType.TOPIC);

        try {
            service.connectParentChild(topic10, topic10, relevance, 0);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        service.connectParentChild(topic10, topic11, relevance, 0);
        service.connectParentChild(topic11, topic12, relevance, 0);

        try {
            service.connectParentChild(topic12, topic10, relevance, 0);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        try {
            service.connectParentChild(topic11, topic10, relevance, 0);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        try {
            service.connectParentChild(topic12, topic11, relevance, 0);
            fail("Expected DuplicateConnectionException");
        } catch (DuplicateConnectionException ignored) {

        }

        try {
            service.connectParentChild(topic11, topic10, relevance, 0);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }
    }

    @Test
    public void connectTopicResource() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);
        final var topic3 = builder.node(NodeType.TOPIC);
        final var topic4 = builder.node(NodeType.TOPIC);

        final var resource1 = builder.resource();
        final var resource2 = builder.resource();
        final var resource3 = builder.resource();
        final var resource4 = builder.resource();
        final var resource5 = builder.resource();
        final var resource6 = builder.resource();
        final var resource7 = builder.resource();

        final var relevance = builder.relevance();

        final var connection1 = service.connectNodeResource(topic1, resource1, relevance, true, null);
        assertNotNull(connection1);
        assertSame(topic1, connection1.getNode().orElse(null));
        assertSame(resource1, connection1.getResource().orElse(null));
        assertTrue(connection1.isPrimary().orElseThrow());
        assertEquals(1, connection1.getRank());

        verify(cachedUrlUpdaterService, atLeastOnce()).updateCachedUrls(resource1);

        final var connection2 = service.connectNodeResource(topic1, resource2, relevance, true, null);
        assertNotNull(connection2);
        assertSame(topic1, connection2.getNode().orElse(null));
        assertSame(resource2, connection2.getResource().orElse(null));
        assertTrue(connection2.isPrimary().orElseThrow());
        assertEquals(2, connection2.getRank());

        assertTrue(connection1.isPrimary().orElseThrow());
        assertEquals(1, connection1.getRank());

        final var connection3 = service.connectNodeResource(topic2, resource2, relevance, false, null);
        assertFalse(connection3.isPrimary().orElseThrow());
        assertEquals(1, connection3.getRank());

        // Has not changed the first connection since setting this to non-primary
        assertTrue(connection2.isPrimary().orElseThrow());

        // Test setting primary, should set old primary to non-primary
        final var connection4 = service.connectNodeResource(topic3, resource2, relevance, true, null);
        assertTrue(connection4.isPrimary().orElseThrow());
        assertEquals(1, connection4.getRank());

        assertFalse(connection3.isPrimary().orElseThrow());
        assertFalse(connection2.isPrimary().orElseThrow());

        // Test ranking
        final var connection5 = service.connectNodeResource(topic4, resource1, relevance, true, null);
        assertEquals(1, connection5.getRank());

        final var connection6 = service.connectNodeResource(topic4, resource2, relevance, true, null);
        assertEquals(1, connection5.getRank());
        assertEquals(2, connection6.getRank());

        final var connection7 = service.connectNodeResource(topic4, resource3, relevance, true, 1);
        assertEquals(2, connection5.getRank());
        assertEquals(3, connection6.getRank());
        assertEquals(1, connection7.getRank());

        final var connection8 = service.connectNodeResource(topic4, resource4, relevance, true, 2);
        assertEquals(3, connection5.getRank());
        assertEquals(4, connection6.getRank());
        assertEquals(1, connection7.getRank());
        assertEquals(2, connection8.getRank());

        final var connection9 = service.connectNodeResource(topic4, resource5, relevance, true, 5);
        assertEquals(3, connection5.getRank());
        assertEquals(4, connection6.getRank());
        assertEquals(1, connection7.getRank());
        assertEquals(2, connection8.getRank());
        assertEquals(5, connection9.getRank());

        // First topic connection for a resource will be primary regardless of request
        final var forcedPrimaryConnection1 = service.connectNodeResource(topic4, resource6, relevance, false, 0);
        assertTrue(forcedPrimaryConnection1.isPrimary().orElseThrow());
        final var forcedPrimaryConnection2 = service.connectNodeResource(topic4, resource7, relevance, false, 1);
        assertTrue(forcedPrimaryConnection2.isPrimary().orElseThrow());

        // Trying to add duplicate connection
        try {
            service.connectNodeResource(topic4, resource4, relevance, false, 0);
            fail("Expected DuplicateConnectionException");
        } catch (DuplicateConnectionException ignored) {
        }
    }

    @Test
    public void disconnectTopicSubtopic() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var subtopic1 = builder.node(NodeType.TOPIC);
        final var subtopic2 = builder.node(NodeType.TOPIC);
        final var subtopic3 = builder.node(NodeType.TOPIC);

        final var topic1subtopic1 = NodeConnection.create(topic1, subtopic1);
        final var topic1subtopic2 = NodeConnection.create(topic1, subtopic2);
        final var topic1subtopic3 = NodeConnection.create(topic1, subtopic3);

        // Just verifies the pre-conditions of the created objects that is used for the test
        assertTrue(topic1.getChildConnections().containsAll(Set.of(topic1subtopic1, topic1subtopic2, topic1subtopic3)));
        assertSame(topic1subtopic1, subtopic1.getParentConnection().orElseThrow());
        assertSame(topic1subtopic2, subtopic2.getParentConnection().orElseThrow());
        assertSame(topic1subtopic3, subtopic3.getParentConnection().orElseThrow());
        assertEquals(3, topic1.getChildConnections().size());

        reset(cachedUrlUpdaterService);

        service.disconnectParentChildConnection(topic1subtopic1);

        verify(cachedUrlUpdaterService).updateCachedUrls(subtopic1);

        assertFalse(topic1subtopic1.getParent().isPresent());
        assertFalse(topic1subtopic1.getChild().isPresent());
        assertFalse(topic1.getChildConnections().contains(topic1subtopic1));
        assertEquals(2, topic1.getChildConnections().size());
        assertFalse(subtopic1.getParentConnection().isPresent());
        assertSame(topic1subtopic2, subtopic2.getParentConnection().orElseThrow());
        assertSame(topic1subtopic3, subtopic3.getParentConnection().orElseThrow());

        service.disconnectParentChild(topic1, subtopic2);
        assertFalse(topic1subtopic2.getParent().isPresent());
        assertFalse(topic1subtopic2.getChild().isPresent());
        assertFalse(topic1.getChildConnections().contains(topic1subtopic2));
        assertEquals(1, topic1.getChildConnections().size());
        assertFalse(subtopic1.getParentConnection().isPresent());
        assertFalse(subtopic2.getParentConnection().isPresent());
        assertSame(topic1subtopic3, subtopic3.getParentConnection().orElseThrow());
    }

    @Test
    public void disconnectTopicResource() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);
        final var topic3 = builder.node(NodeType.TOPIC);

        final var resource1 = builder.resource();
        final var resource2 = builder.resource();
        final var resource3 = builder.resource();

        final var topic1resource1 = NodeResource.create(topic1, resource1, true);
        final var topic1resource2 = NodeResource.create(topic1, resource2, true);
        final var topic1resource3 = NodeResource.create(topic1, resource3, true);

        final var topic2resource1 = NodeResource.create(topic2, resource1, false);
        final var topic3resource1 = NodeResource.create(topic3, resource1, false);

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());
        assertFalse(topic3resource1.isPrimary().orElseThrow());

        assertTrue(topic1.getNodeResources().contains(topic1resource1));
        assertTrue(resource1.getNodeResources().contains(topic1resource1));

        reset(cachedUrlUpdaterService);

        service.disconnectNodeResource(topic1, resource1);

        verify(cachedUrlUpdaterService, atLeastOnce()).updateCachedUrls(resource1);

        assertTrue(topic2resource1.isPrimary().orElseThrow() ^ topic3resource1.isPrimary().orElseThrow());
        assertFalse(topic1resource1.getResource().isPresent());
        assertFalse(topic1resource1.getNode().isPresent());
        assertFalse(topic1.getNodeResources().contains(topic1resource1));
        assertFalse(resource1.getNodeResources().contains(topic1resource1));

        service.disconnectNodeResource(topic2resource1);
        assertTrue(topic3resource1.isPrimary().orElseThrow());

        assertTrue(resource2.getNodeResources().contains(topic1resource2));
        assertTrue(resource3.getNodeResources().contains(topic1resource3));

        service.disconnectNodeResource(topic1resource2);
        service.disconnectNodeResource(topic1resource3);

        assertFalse(resource2.getNodeResources().contains(topic1resource2));
        assertFalse(resource3.getNodeResources().contains(topic1resource3));
    }

    @Test
    public void updateTopicSubtopic() {
        final var rootTopic1 = builder.node(NodeType.TOPIC);
        final var rootTopic3 = builder.node(NodeType.TOPIC);

        final var subTopic1 = builder.node(NodeType.TOPIC);
        final var subTopic2 = builder.node(NodeType.TOPIC);
        final var subTopic3 = builder.node(NodeType.TOPIC);

        final var subject1 = builder.node(NodeType.SUBJECT);

        final var relevance = builder.relevance();

        final var subjectTopic = NodeConnection.create(subject1, subTopic2);

        final var connection1 = NodeConnection.create(rootTopic1, subTopic1);

        final var connection5 = NodeConnection.create(rootTopic3, subTopic3);

        connection1.setRank(1);

        assertEquals(1, connection1.getRank());

        service.updateParentChild(connection1, relevance, 2);
        assertEquals(2, connection1.getRank());
    }

    @Test
    public void updateTopicResource() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);

        final var resource1 = builder.resource();
        final var resource2 = builder.resource();
        final var resource3 = builder.resource();

        final var topic1resource1 = NodeResource.create(topic1, resource1, true);
        final var topic1resource2 = NodeResource.create(topic1, resource2, true);
        final var topic1resource3 = NodeResource.create(topic1, resource3, true);

        final var topic2resource1 = NodeResource.create(topic2, resource1, false);

        final var relevance = builder.relevance();

        topic1resource1.setRank(1);
        topic1resource2.setRank(2);
        topic1resource3.setRank(3);

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());

        service.updateNodeResource(topic2resource1, relevance, true, null);

        assertFalse(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertTrue(topic2resource1.isPrimary().orElseThrow());

        service.updateNodeResource(topic2resource1, relevance, false, null);

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());

        try {
            service.updateNodeResource(topic1resource3, relevance, false, null);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        assertEquals(1, topic1resource1.getRank());
        assertEquals(2, topic1resource2.getRank());
        assertEquals(3, topic1resource3.getRank());
        service.updateNodeResource(topic1resource3, relevance, true, 1);
        assertEquals(2, topic1resource1.getRank());
        assertEquals(3, topic1resource2.getRank());
        assertEquals(1, topic1resource3.getRank());

        service.updateNodeResource(topic1resource2, relevance, true, 2);
        assertEquals(3, topic1resource1.getRank());
        assertEquals(2, topic1resource2.getRank());
        assertEquals(1, topic1resource3.getRank());
    }

    @Test
    public void replacePrimaryConnectionsFor() {
        final var subject1 = builder.node(NodeType.SUBJECT);
        final var subject2 = builder.node(NodeType.SUBJECT);

        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);

        final var resource1 = builder.resource();

        NodeConnection.create(subject1, topic1);

        final var topic1resource1 = NodeResource.create(topic1, resource1, true);
        final var topic2resource1 = NodeResource.create(topic2, resource1, false);
        // final var topic1topic2 = NodeConnection.create(topic1, topic2);

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());

        service.replacePrimaryConnectionsFor(topic1);

        assertFalse(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic2resource1.isPrimary().orElseThrow());
    }
}
