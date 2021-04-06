package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.rest.v1.commands.NodeCommand;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.TopicDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.persistence.EntityManager;
import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NodesTest extends RestTest {
    @Autowired
    EntityManager entityManager;

    @Autowired
    private TestSeeder testSeeder;

    @BeforeEach
    void clearAllRepos() {
        resourceRepository.deleteAllAndFlush();
        topicRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_single_node() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .name("trigonometry")
                        .contentUri("urn:article:1")
                        .publicId("urn:topic:1")
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("trigonometry", node.getName());
        assertEquals("urn:article:1", node.getContentUri().toString());
        assertEquals("/subject:1/topic:1", node.getPath());
        assertEquals("urn:nodetype:topic", node.getNodeType().toString());

        assertNotNull(node.getMetadata());
        assertTrue(node.getMetadata().isVisible());
        assertTrue(node.getMetadata().getGrepCodes().size() == 1 && node.getMetadata().getGrepCodes().contains("TOPIC1"));
    }

    @Test
    public void topic_without_subject_has_no_url() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1");
        final var topic = testUtils.getObject(TopicDTO.class, response);

        assertNull(topic.getPath());
    }

    @Test
    public void can_get_nodes_by_contentURI() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(t -> {
                    t.name("photo synthesis");
                    t.contentUri(URI.create("urn:test:1"));
                }));
        builder.subject(s -> s
                .name("Maths")
                .topic(t -> {
                    t.name("trigonometry");
                    t.contentUri(URI.create("urn:test:2"));
                }));

        {
            final var response = testUtils.getResource("/v1/nodes?contentURI=urn:test:1");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("photo synthesis", nodes[0].getName());
        }

        {
            final var response = testUtils.getResource("/v1/nodes?contentURI=urn:test:2");
            final var nodes = testUtils.getObject(TopicDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("trigonometry", nodes[0].getName());
        }
    }

    @Test
    public void can_get_nodes_by_contentURI_and_nodeType() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(t -> {
                    t.name("photo synthesis");
                    t.contentUri(URI.create("urn:test:1"));
                }));
        builder.subject(s -> s
                .name("Maths")
                .topic(t -> {
                    t.name("trigonometry");
                    t.contentUri(URI.create("urn:test:2"));
                }));

        {
            final var response = testUtils.getResource("/v1/nodes?contentURI=urn:test:1&nodeType=urn:nodetype:subject");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(0, nodes.length);
        }
        {
            final var response = testUtils.getResource("/v1/nodes?contentURI=urn:test:1&nodeType=urn:nodetype:topic");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("photo synthesis", nodes[0].getName());
        }

        {
            final var response = testUtils.getResource("/v1/nodes?contentURI=urn:test:2&nodeType=urn:nodetype:subject");
            final var nodes = testUtils.getObject(TopicDTO[].class, response);
            assertEquals(0, nodes.length);
        }
        {
            final var response = testUtils.getResource("/v1/nodes?contentURI=urn:test:2&nodeType=urn:nodetype:topic");
            final var nodes = testUtils.getObject(TopicDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("trigonometry", nodes[0].getName());
        }
    }


    @Test
    public void can_get_all_nodes() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(t -> t.name("photo synthesis")));
        builder.subject(s -> s
                .name("Maths")
                .topic(t -> t.name("trigonometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes");
        final var nodes = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(4, nodes.length);

        assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
        assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
        assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
        assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
        assertAnyTrue(nodes, t -> "urn:nodetype:topic".equals(t.getNodeType().toString()));
        assertAnyTrue(nodes, t -> "urn:nodetype:subject".equals(t.getNodeType().toString()));
        assertAllTrue(nodes, t -> isValidId(t.getId()));
        assertAllTrue(nodes, t -> t.getPath().contains("subject"));
        assertAnyTrue(nodes, t -> t.getPath().contains("subject") && t.getPath().contains("topic"));

        assertAllTrue(nodes, t -> t.getMetadata() != null);
        assertAllTrue(nodes, t -> t.getMetadata().isVisible());
        assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().size() == 1);
    }


    @Test
    public void can_get_all_topic_nodes() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(t -> t.name("photo synthesis")));
        builder.subject(s -> s
                .name("Maths")
                .topic(t -> t.name("trigonometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes?nodeType=urn:nodetype:topic");
        final var nodes = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(2, nodes.length);

        assertAllTrue(nodes, t -> !"Basic science".equals(t.getName()));
        assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
        assertAllTrue(nodes, t -> !"Maths".equals(t.getName()));
        assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
        assertAllTrue(nodes, t -> "urn:nodetype:topic".equals(t.getNodeType().toString()));
        assertAllTrue(nodes, t -> !"urn:nodetype:subject".equals(t.getNodeType().toString()));
        assertAllTrue(nodes, t -> isValidId(t.getId()));
        assertAllTrue(nodes, t -> t.getPath().contains("subject") && t.getPath().contains("topic"));

        assertAllTrue(nodes, t -> t.getMetadata() != null);
        assertAllTrue(nodes, t -> t.getMetadata().isVisible());
        assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().size() == 1);
    }

    @Test
    public void can_get_all_subject_nodes() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(t -> t.name("photo synthesis")));
        builder.subject(s -> s
                .name("Maths")
                .topic(t -> t.name("trigonometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes?nodeType=urn:nodetype:subject");
        final var nodes = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(2, nodes.length);

        assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
        assertAllTrue(nodes, t -> !"photo synthesis".equals(t.getName()));
        assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
        assertAllTrue(nodes, t -> !"trigonometry".equals(t.getName()));
        assertAllTrue(nodes, t -> !"urn:nodetype:topic".equals(t.getNodeType().toString()));
        assertAllTrue(nodes, t -> "urn:nodetype:subject".equals(t.getNodeType().toString()));
        assertAllTrue(nodes, t -> isValidId(t.getId()));
        assertAllTrue(nodes, t -> t.getPath().contains("subject"));

        assertAllTrue(nodes, t -> t.getMetadata() != null);
        assertAllTrue(nodes, t -> t.getMetadata().isVisible());
        assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().size() == 1);
    }


    /**
     * This test creates a structure of subjects and topics as follows:
     * <pre>
     *   S:1
     *    \
     *     T:1
     *      \
     *       T:2
     *      /  \
     *    T:3   T:4
     * </pre>
     * <p>
     * S:1 = urn:subject:1000
     * S:2 = urn:subject:2000
     * T:1 = urn:topic:1000
     * T:2 = urn:topic:2000
     * T:3 = urn:topic:3000
     * T:4 = urn:topic:4000
     * <p>
     * The test examines the T:2 node and verifies that it reports the correct parent-subject, parent-topic and
     * subtopic connections. As shown in the figure above, it should have 1 parent-subject (S:2), 1 parent-topic (T:1),
     * and 2 subtopics (T:3 and T:4).
     */
    @Test
    public void can_get_all_connections() throws Exception {
        testSeeder.topicConnectionsTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:2000/connections");
        ConnectionIndexDTO[] connections = testUtils.getObject(ConnectionIndexDTO[].class, response);

        assertEquals(3, connections.length, "Correct number of connections");
        assertAllTrue(connections, c -> c.getPaths().size() > 0); //all connections have at least one path

        connectionsHaveCorrectTypes(connections);
    }

    @Test
    public void subnodes_are_sorted_by_rank() throws Exception {
        testSeeder.subtopicsByTopicIdAndFiltersTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes");
        final var subnodes = testUtils.getObject(TopicDTO[].class, response);
        assertEquals(7, subnodes.length);

        assertEquals("urn:topic:2", subnodes[0].getId().toString());
        assertEquals("urn:topic:3", subnodes[1].getId().toString());
        assertEquals("urn:topic:4", subnodes[2].getId().toString());
        assertEquals("urn:topic:5", subnodes[3].getId().toString());
        assertEquals("urn:topic:6", subnodes[4].getId().toString());
        assertEquals("urn:topic:7", subnodes[5].getId().toString());
    }

    @Test
    public void can_get_unfiltered_subnodes() throws Exception {
        testSeeder.subtopicsByTopicIdAndFiltersTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes");
        final var subnodes = testUtils.getObject(TopicDTO[].class, response);
        assertEquals(7, subnodes.length, "Unfiltered subtopics");

        assertAllTrue(subnodes, subnode -> subnode.getMetadata() != null);
        assertAllTrue(subnodes, subnode -> subnode.getMetadata().isVisible());
        assertAllTrue(subnodes, subnode -> subnode.getMetadata().getGrepCodes().size() == 1);
    }

    private void connectionsHaveCorrectTypes(ConnectionIndexDTO[] connections) {
        ConnectionTypeCounter connectionTypeCounter = new ConnectionTypeCounter(connections).countTypes();
        assertEquals(1, connectionTypeCounter.getParentCount());
        assertEquals(2, connectionTypeCounter.getChildCount());
    }

    @Test
    public void can_create_topic() throws Exception {
        final var createTopicCommand = new NodeCommand() {{
            name = "trigonometry";
            contentUri = URI.create("urn:article:1");
            nodeType = URI.create("urn:nodetype:topic");
        }};

        MockHttpServletResponse response = testUtils.createResource("/v1/nodes", createTopicCommand);
        URI id = getId(response);

        Topic topic = topicRepository.getByPublicId(id);
        assertEquals(createTopicCommand.name, topic.getName());
        assertEquals(createTopicCommand.contentUri, topic.getContentUri());
    }

    @Test
    public void can_create_topic_with_id() throws Exception {
        final var createTopicCommand = new NodeCommand() {{
            id = URI.create("urn:topic:1");
            name = "trigonometry";
            nodeType = URI.create("urn:nodetype:topic");
        }};

        testUtils.createResource("/v1/nodes", createTopicCommand);

        Topic topic = topicRepository.getByPublicId(createTopicCommand.id);
        assertEquals(createTopicCommand.name, topic.getName());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new NodeCommand() {{
            id = URI.create("urn:topic:1");
            name = "name";
            nodeType = URI.create("urn:nodetype:topic");
        }};

        testUtils.createResource("/v1/nodes", command, status().isCreated());
        testUtils.createResource("/v1/nodes", command, status().isConflict());
    }

    @Test
    public void can_update_topic() throws Exception {
        URI id = builder.topic().getPublicId();

        testUtils.updateResource("/v1/nodes/" + id, new NodeCommand() {{
            name = "trigonometry";
            contentUri = URI.create("urn:article:1");
            nodeType = URI.create("urn:nodetype:topic");
        }});

        Topic topic = topicRepository.getByPublicId(id);
        assertEquals("trigonometry", topic.getName());
        assertEquals("urn:article:1", topic.getContentUri().toString());
        assertEquals("urn:nodetype:topic", topic.getNodeType().map(NodeType::getPublicId).map(URI::toString).orElse(null));
    }

    @Test
    public void can_delete_topic_with_2_subtopics() throws Exception {
        Topic childTopic1 = builder.topic(child -> child.name("DELETE EDGE TO ME"));
        Topic childTopic2 = builder.topic(child -> child.name("DELETE EDGE TO ME ALSO"));

        URI parentId = builder.topic(parent -> parent
                .subtopic(childTopic1)
                .subtopic(childTopic2)
        ).getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(topicRepository.findByPublicId(parentId));

        verify(metadataApiService).deleteMetadataByPublicId(parentId);
    }

    @Test
    public void can_delete_topic_with_2_resources() throws Exception {
        Topic topic = builder.topic(child -> child
                .name("MAIN TOPIC")
                .translation("nb", tr -> tr.name("HovedEmne"))
                .resource(r -> r.publicId("urn:resource:1"))
                .resource(r -> r.publicId("urn:resource:2")));

        final var topicId = topic.getPublicId();

        testUtils.deleteResource("/v1/nodes/" + topicId);

        assertNull(topicRepository.findByPublicId(topicId));

        verify(metadataApiService).deleteMetadataByPublicId(topicId);
    }

    @Test
    public void can_delete_topic_but_subtopics_remain() throws Exception {
        Topic childTopic = builder.topic(child -> child
                .name("DELETE EDGE TO ME")
                .translation("nb", tr -> tr.name("emne"))
                .subtopic(sub -> sub.publicId("urn:topic:1"))
                .resource(r -> r.publicId("urn:resource:1")));

        URI parentId = builder.topic(parent -> parent
                .subtopic(childTopic)
        ).getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(topicRepository.findByPublicId(parentId));
        assertNotNull(topicRepository.findByPublicId(childTopic.getPublicId()));

        verify(metadataApiService).deleteMetadataByPublicId(parentId);
    }

    @Test
    public void can_delete_topic_but_resources_remain() throws Exception {
        Resource resource = builder.resource("resource", r -> r
                .translation("nb", tr -> tr.name("ressurs"))
                .resourceType(rt -> rt.name("Learning path")));

        URI parentId = builder.topic(parent -> parent
                .resource(resource)
        ).getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(topicRepository.findByPublicId(parentId));
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
