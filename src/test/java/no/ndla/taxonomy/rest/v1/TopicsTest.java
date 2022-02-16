/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.rest.v1.commands.TopicCommand;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.EntityWithPathDTO;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.Set;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicsTest extends RestTest {
    @Autowired
    EntityManager entityManager;

    @Autowired
    private TestSeeder testSeeder;

    @BeforeEach
    void clearAllRepos() {
        resourceRepository.deleteAllAndFlush();
        nodeRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_single_topic() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).publicId("urn:subject:1").child(t -> t
                .nodeType(NodeType.TOPIC).name("trigonometry").contentUri("urn:article:1").publicId("urn:topic:1")));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1");
        final var topic = testUtils.getObject(NodeDTO.class, response);

        assertEquals("trigonometry", topic.getName());
        assertEquals("urn:article:1", topic.getContentUri().toString());
        assertEquals("/subject:1/topic:1", topic.getPath());

        assertNotNull(topic.getMetadata());
        assertTrue(topic.getMetadata().isVisible());
        assertTrue(topic.getMetadata().getGrepCodes().size() == 1
                && topic.getMetadata().getGrepCodes().contains("TOPIC1"));
    }

    @Test
    public void topic_without_subject_has_no_url() throws Exception {
        builder.node(NodeType.TOPIC, t -> t.publicId("urn:topic:1"));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1");
        final var topic = testUtils.getObject(NodeDTO.class, response);

        assertEquals("", topic.getPath());
    }

    @Test
    public void can_get_topics_by_contentURI() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> {
            t.name("photo synthesis");
            t.contentUri(URI.create("urn:test:1"));
        }));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> {
            t.name("trigonometry");
            t.contentUri(URI.create("urn:test:2"));
        }));

        {
            final var response = testUtils.getResource("/v1/topics?contentURI=urn:test:1");
            final var topics = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, topics.length);
            assertEquals("photo synthesis", topics[0].getName());
        }

        {
            final var response = testUtils.getResource("/v1/topics?contentURI=urn:test:2");
            final var topics = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, topics.length);
            assertEquals("trigonometry", topics[0].getName());
        }
    }

    @Test
    public void can_get_topics_by_key_and_value() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> {
            t.publicId("urn:topic:b8001");
            t.name("photo synthesis");
            t.contentUri(URI.create("urn:test:1"));
        }));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> {
            t.publicId("urn:topic:b8003");
            t.name("trigonometry");
            t.contentUri(URI.create("urn:test:2"));
        }));

        final var metadata1 = new MetadataDto();
        metadata1.setPublicId("urn:topic:b8001");
        metadata1.setGrepCodes(Set.of("GREP1"));
        final var metadata2 = new MetadataDto();
        metadata2.setPublicId("urn:topic:b8003");
        metadata2.setGrepCodes(Set.of("GREP2"));
        when(metadataApiService.getMetadataByKeyAndValue("test", "value")).thenReturn(Set.of(metadata1));
        when(metadataApiService.getMetadataByKeyAndValue("test", "value2")).thenReturn(Set.of(metadata2));

        {
            final var response = testUtils.getResource("/v1/topics?key=test&value=value");
            final var topics = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, topics.length);
            assertEquals("photo synthesis", topics[0].getName());
            assertNotNull(topics[0].getMetadata());
            assertNotNull(topics[0].getMetadata().getGrepCodes());
            assertEquals(Set.of("GREP1"), topics[0].getMetadata().getGrepCodes());
        }

        {
            final var response = testUtils.getResource("/v1/topics?key=test&value=value2");
            final var topics = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, topics.length);
            assertEquals("trigonometry", topics[0].getName());
            assertNotNull(topics[0].getMetadata());
            assertNotNull(topics[0].getMetadata().getGrepCodes());
            assertEquals(Set.of("GREP2"), topics[0].getMetadata().getGrepCodes());
        }

        verify(metadataApiService, times(1)).getMetadataByKeyAndValue("test", "value");
        verify(metadataApiService, times(1)).getMetadataByKeyAndValue("test", "value2");
    }

    @Test
    public void can_get_all_topics() throws Exception {
        builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> t.name("trigonometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics");
        final var topics = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(2, topics.length);

        assertAnyTrue(topics, t -> "photo synthesis".equals(t.getName()));
        assertAnyTrue(topics, t -> "trigonometry".equals(t.getName()));
        assertAllTrue(topics, t -> isValidId(t.getId()));
        assertAllTrue(topics, t -> t.getPath().contains("subject") && t.getPath().contains("topic"));

        assertAllTrue(topics, t -> t.getMetadata() != null);
        assertAllTrue(topics, t -> t.getMetadata().isVisible());
        assertAllTrue(topics, t -> t.getMetadata().getGrepCodes().size() == 1);
    }

    /**
     * This test creates a structure of subjects and topics as follows:
     *
     * <pre>
     *   S:1
     *    \
     *     T:1
     *      \
     *       T:2
     *      /  \
     *    T:3   T:4
     * </pre>
     *
     * <p>
     * S:1 = urn:subject:1000 S:2 = urn:subject:2000 T:1 = urn:topic:1000 T:2 = urn:topic:2000 T:3 = urn:topic:3000 T:4
     * = urn:topic:4000
     *
     * <p>
     * The test examines the T:2 node and verifies that it reports the correct parent-subject, parent-topic and subtopic
     * connections. As shown in the figure above, it should have 1 parent-subject (S:2), 1 parent-topic (T:1), and 2
     * subtopics (T:3 and T:4).
     */
    @Test
    public void can_get_all_connections() throws Exception {
        testSeeder.topicNodeConnectionsTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:2000/connections");
        ConnectionIndexDTO[] connections = testUtils.getObject(ConnectionIndexDTO[].class, response);

        assertEquals(3, connections.length, "Correct number of connections");
        assertAllTrue(connections, c -> c.getPaths().size() > 0); // all connections have at least one path

        connectionsHaveCorrectTypes(connections);
    }

    @Test
    public void subtopics_are_sorted_by_rank() throws Exception {
        testSeeder.subtopicsByNodeIdAndRelevanceTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/topics");
        final var subtopics = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(7, subtopics.length);

        assertEquals("urn:topic:2", subtopics[0].getId().toString());
        assertEquals("urn:topic:3", subtopics[1].getId().toString());
        assertEquals("urn:topic:4", subtopics[2].getId().toString());
        assertEquals("urn:topic:5", subtopics[3].getId().toString());
        assertEquals("urn:topic:6", subtopics[4].getId().toString());
        assertEquals("urn:topic:7", subtopics[5].getId().toString());
    }

    @Test
    public void can_get_unfiltered_subtopics() throws Exception {
        testSeeder.subtopicsByNodeIdAndRelevanceTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/topics");
        final var subtopics = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(7, subtopics.length, "Unfiltered subtopics");

        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata() != null);
        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata().isVisible());
        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata().getGrepCodes().size() == 1);
    }

    private void connectionsHaveCorrectTypes(ConnectionIndexDTO[] connections) {
        ConnectionTypeCounter connectionTypeCounter = new ConnectionTypeCounter(connections).countTypes();
        assertEquals(1, connectionTypeCounter.getParentCount());
        assertEquals(2, connectionTypeCounter.getChildCount());
    }

    @Test
    public void can_create_topic() throws Exception {
        final var createTopicCommand = new TopicCommand() {
            {
                name = "trigonometry";
                contentUri = URI.create("urn:article:1");
            }
        };

        MockHttpServletResponse response = testUtils.createResource("/v1/topics", createTopicCommand);
        URI id = getId(response);

        Node topic = nodeRepository.getByPublicId(id);
        assertEquals(createTopicCommand.name, topic.getName());
        assertEquals(createTopicCommand.contentUri, topic.getContentUri());
    }

    @Test
    public void can_create_topic_with_id() throws Exception {
        final var createTopicCommand = new TopicCommand() {
            {
                id = URI.create("urn:topic:1");
                name = "trigonometry";
            }
        };

        testUtils.createResource("/v1/topics", createTopicCommand);

        Node topic = nodeRepository.getByPublicId(createTopicCommand.id);
        assertEquals(createTopicCommand.name, topic.getName());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new TopicCommand() {
            {
                id = URI.create("urn:topic:1");
                name = "name";
            }
        };

        testUtils.createResource("/v1/topics", command, status().isCreated());
        testUtils.createResource("/v1/topics", command, status().isConflict());
    }

    @Test
    public void can_update_topic() throws Exception {
        URI publicId = builder.node(NodeType.TOPIC).getPublicId();

        testUtils.updateResource("/v1/topics/" + publicId, new TopicCommand() {
            {
                id = publicId;
                name = "trigonometry";
                contentUri = URI.create("urn:article:1");
            }
        });

        Node topic = nodeRepository.getByPublicId(publicId);
        assertEquals("trigonometry", topic.getName());
        assertEquals("urn:article:1", topic.getContentUri().toString());
    }

    @Test
    public void can_update_topic_with_new_id() throws Exception {
        URI publicId = builder.node(NodeType.TOPIC).getPublicId();
        URI randomId = URI.create("urn:topic:random");

        testUtils.updateResource("/v1/topics/" + publicId, new TopicCommand() {
            {
                id = randomId;
                name = "trigonometry";
                contentUri = URI.create("urn:article:1");
            }
        });

        Node topic = nodeRepository.getByPublicId(randomId);
        assertEquals("trigonometry", topic.getName());
        assertEquals("urn:article:1", topic.getContentUri().toString());
    }

    @Test
    public void can_delete_topic_with_2_subtopics() throws Exception {
        Node childTopic1 = builder.node(NodeType.TOPIC).name("DELETE EDGE TO ME");
        Node childTopic2 = builder.node(NodeType.TOPIC).name("DELETE EDGE TO ME ALSO");

        URI parentId = builder.node(parent -> parent.nodeType(NodeType.TOPIC).child(childTopic1).child(childTopic2))
                .getPublicId();

        testUtils.deleteResource("/v1/topics/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));

        verify(metadataApiService).deleteMetadataByPublicId(parentId);
    }

    @Test
    public void can_delete_topic_with_2_resources() throws Exception {
        Node topic = builder.node(NodeType.TOPIC,
                child -> child.name("MAIN TOPIC").translation("nb", tr -> tr.name("HovedEmne"))
                        .resource(r -> r.publicId("urn:resource:1")).resource(r -> r.publicId("urn:resource:2")));

        final var topicId = topic.getPublicId();

        testUtils.deleteResource("/v1/topics/" + topicId);

        assertNull(nodeRepository.findByPublicId(topicId));

        verify(metadataApiService).deleteMetadataByPublicId(topicId);
    }

    @Test
    public void can_delete_topic_but_subtopics_remain() throws Exception {
        Node childTopic = builder.node(NodeType.TOPIC,
                child -> child.name("DELETE EDGE TO ME").translation("nb", tr -> tr.name("emne"))
                        .child(NodeType.TOPIC, sub -> sub.publicId("urn:topic:1"))
                        .resource(r -> r.publicId("urn:resource:1")));

        URI parentId = builder.node(NodeType.TOPIC, parent -> parent.child(childTopic)).getPublicId();

        testUtils.deleteResource("/v1/topics/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
        assertNotNull(nodeRepository.findByPublicId(childTopic.getPublicId()));

        verify(metadataApiService).deleteMetadataByPublicId(parentId);
    }

    @Test
    public void can_delete_topic_but_resources_and_filter_remain() throws Exception {
        Resource resource = builder.resource("resource",
                r -> r.translation("nb", tr -> tr.name("ressurs")).resourceType(rt -> rt.name("Learning path")));

        URI parentId = builder.node(NodeType.TOPIC, parent -> parent.resource(resource)).getPublicId();

        testUtils.deleteResource("/v1/topics/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
        assertNotNull(resourceRepository.findByPublicId(resource.getPublicId()));

        verify(metadataApiService).deleteMetadataByPublicId(parentId);
    }

    private static class ConnectionTypeCounter {
        private final ConnectionIndexDTO[] connections;
        private int subjectCount;
        private int parentCount;
        private int childCount;

        ConnectionTypeCounter(ConnectionIndexDTO[] connections) {
            this.connections = connections;
        }

        int getSubjectCount() {
            return subjectCount;
        }

        int getParentCount() {
            return parentCount;
        }

        int getChildCount() {
            return childCount;
        }

        ConnectionTypeCounter countTypes() {
            subjectCount = 0;
            parentCount = 0;
            childCount = 0;
            for (ConnectionIndexDTO connection : connections) {
                switch (connection.getType()) {
                case "parent-subject":
                    subjectCount++;
                    break;
                case "parent-topic":
                    parentCount++;
                    break;
                case "subtopic":
                    childCount++;
                    break;
                default:
                    fail("Unexpected connection type :" + connection.getType());
                }
            }
            return this;
        }
    }
}
