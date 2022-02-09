/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.commands.NodeCommand;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.TopicChildDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.stream.Collectors;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NodesTest extends RestTest {
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
    public void can_get_single_node() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).publicId("urn:subject:1").child(t -> t
                .nodeType(NodeType.TOPIC).name("trigonometry").contentUri("urn:article:1").publicId("urn:topic:1")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("trigonometry", node.getName());
        assertEquals("urn:article:1", node.getContentUri().toString());
        assertEquals("/subject:1/topic:1", node.getPath());

        assertNotNull(node.getMetadata());
        assertTrue(node.getMetadata().isVisible());
    }

    @Test
    public void single_node_has_no_url() throws Exception {
        builder.node(NodeType.NODE, t -> t.publicId("urn:node:1"));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:node:1");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertNull(node.getPath());
    }

    @Test
    public void can_get_nodes_by_contentURI() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> {
            t.name("photo synthesis");
            t.contentUri(URI.create("urn:test:1"));
        }));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> {
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
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("trigonometry", nodes[0].getName());
        }
    }

    /*
     * @Test public void can_get_nodes_by_key_and_value() throws Exception { builder.node(NodeType.SUBJECT, s ->
     * s.isContext(true).name("Basic science").child(t -> { t.nodeType(NodeType.TOPIC); t.publicId("urn:topic:b8001");
     * t.name("photo synthesis"); t.contentUri(URI.create("urn:test:1")); })); builder.node(NodeType.SUBJECT, s ->
     * s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> { t.publicId("urn:topic:b8003");
     * t.name("trigonometry"); t.contentUri(URI.create("urn:test:2")); }));
     * 
     * final var metadata1 = new MetadataDto(); metadata1.setPublicId("urn:topic:b8001");
     * metadata1.setGrepCodes(Set.of("GREP1")); final var metadata2 = new MetadataDto();
     * metadata2.setPublicId("urn:topic:b8003"); metadata2.setGrepCodes(Set.of("GREP2"));
     * when(metadataApiService.getMetadataByKeyAndValue("test", "value")).thenReturn(Set.of(metadata1));
     * when(metadataApiService.getMetadataByKeyAndValue("test", "value2")).thenReturn(Set.of(metadata2));
     * 
     * { final var response = testUtils.getResource("/v1/nodes?key=test&value=value"); final var nodes =
     * testUtils.getObject(NodeDTO[].class, response); assertEquals(1, nodes.length); assertEquals("photo synthesis",
     * nodes[0].getName()); assertNotNull(nodes[0].getMetadata()); assertNotNull(nodes[0].getMetadata().getGrepCodes());
     * assertEquals(Set.of("GREP1"), nodes[0].getMetadata().getGrepCodes()); }
     * 
     * { final var response = testUtils.getResource("/v1/nodes?key=test&value=value2"); final var nodes =
     * testUtils.getObject(NodeDTO[].class, response); assertEquals(1, nodes.length); assertEquals("trigonometry",
     * nodes[0].getName()); assertNotNull(nodes[0].getMetadata()); assertNotNull(nodes[0].getMetadata().getGrepCodes());
     * assertEquals(Set.of("GREP2"), nodes[0].getMetadata().getGrepCodes()); }
     * 
     * verify(metadataApiService, times(1)).getMetadataByKeyAndValue("test", "value"); verify(metadataApiService,
     * times(1)).getMetadataByKeyAndValue("test", "value2"); }
     */

    @Test
    public void can_get_all_nodes() throws Exception {
        builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> t.name("trigonometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes");
        final var nodes = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(4, nodes.length);

        assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
        assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
        assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
        assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
        assertAllTrue(nodes, t -> isValidId(t.getId()));
        assertAllTrue(nodes, t -> t.getPath().contains("subject"));

        assertAllTrue(nodes, t -> t.getMetadata() != null);
        assertAllTrue(nodes, t -> t.getMetadata().isVisible());
        assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().size() == 0);
    }

    @Test
    public void can_get_all_root_nodes() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isRoot(true).isContext(true).name("Basic science").child(NodeType.TOPIC,
                t -> t.name("photo synthesis")));
        builder.node(NodeType.SUBJECT,
                s -> s.isRoot(true).isContext(true).name("Maths").child(NodeType.TOPIC, t -> t.name("trigonometry")));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Arts and crafts"));
        builder.node(NodeType.NODE,
                n -> n.isRoot(true).name("Random node").child(NodeType.NODE, c -> c.name("Subnode")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes?isRoot=true");
        final var nodes = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(3, nodes.length);

        assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
        assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
        assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
        assertAnyTrue(nodes, t -> t.getPath().contains("subject"));
        assertAllTrue(nodes, t -> isValidId(t.getId()));

        assertAllTrue(nodes, t -> t.getMetadata() != null);
        assertAllTrue(nodes, t -> t.getMetadata().isVisible());
        assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().size() == 0);
    }

    @Test
    public void can_place_subject_below_subject() throws Exception {
        builder.node(NodeType.SUBJECT,
                s -> s.isContext(true).name("Maths").publicId("urn:subject:1").child(NodeType.SUBJECT,
                        t -> t.name("Maths vg1").contentUri("urn:frontpage:1").publicId("urn:subject:2")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:subject:2");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("Maths vg1", node.getName());
        assertEquals("urn:frontpage:1", node.getContentUri().toString());
        assertEquals("/subject:1/subject:2", node.getPath());

        assertNotNull(node.getMetadata());
        assertTrue(node.getMetadata().isVisible());
        assertTrue(node.getMetadata().getGrepCodes().size() == 0);
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
     * <p>
     * S:1 = urn:subject:1000 S:2 = urn:subject:2000 T:1 = urn:topic:1000 T:2 = urn:topic:2000 T:3 = urn:topic:3000 T:4
     * = urn:topic:4000
     * <p>
     * The test examines the T:2 node and verifies that it reports the correct parent-subject, parent-topic and subtopic
     * connections. As shown in the figure above, it should have 1 parent-subject (S:2), 1 parent-topic (T:1), and 2
     * subtopics (T:3 and T:4).
     */
    @Test
    public void can_get_all_connections() throws Exception {
        testSeeder.topicNodeConnectionsTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:2000/connections");
        ConnectionIndexDTO[] connections = testUtils.getObject(ConnectionIndexDTO[].class, response);

        assertEquals(3, connections.length, "Correct number of connections");
        assertAllTrue(connections, c -> c.getPaths().size() > 0); // all connections have at least one path

        connectionsHaveCorrectTypes(connections);
    }

    @Test
    public void subnodes_are_sorted_by_rank() throws Exception {
        testSeeder.subtopicsByNodeIdAndRelevanceTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes");
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
    public void can_get_unfiltered_subnodes() throws Exception {
        testSeeder.subtopicsByNodeIdAndRelevanceTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes");
        final var subtopics = testUtils.getObject(TopicChildDTO[].class, response);
        assertEquals(7, subtopics.length, "Unfiltered subtopics");

        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata() != null);
        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata().isVisible());
        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata().getGrepCodes().size() == 0);
    }

    private void connectionsHaveCorrectTypes(ConnectionIndexDTO[] connections) {
        ConnectionTypeCounter connectionTypeCounter = new ConnectionTypeCounter(connections).countTypes();
        assertEquals(1, connectionTypeCounter.getParentCount());
        assertEquals(2, connectionTypeCounter.getChildCount());
    }

    @Test
    public void can_create_node() throws Exception {
        final var createNodeCommand = new NodeCommand() {
            {
                nodeType = NodeType.NODE;
                name = "node";
                contentUri = URI.create("urn:article:1");
                root = Boolean.TRUE;
            }
        };

        MockHttpServletResponse response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(createNodeCommand.nodeType, node.getNodeType());
        assertEquals(createNodeCommand.name, node.getName());
        assertEquals(createNodeCommand.contentUri, node.getContentUri());
        assertEquals(createNodeCommand.root, node.isRoot());
    }

    @Test
    public void can_create_topic() throws Exception {
        final var createNodeCommand = new NodeCommand() {
            {
                nodeType = NodeType.TOPIC;
                name = "trigonometry";
                contentUri = URI.create("urn:article:1");
            }
        };

        MockHttpServletResponse response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(createNodeCommand.nodeType, node.getNodeType());
        assertEquals(createNodeCommand.name, node.getName());
        assertEquals(createNodeCommand.contentUri, node.getContentUri());
    }

    @Test
    public void can_create_subject() throws Exception {
        final var createNodeCommand = new NodeCommand() {
            {
                nodeType = NodeType.SUBJECT;
                name = "Maths";
                contentUri = URI.create("urn:frontpage:1");
            }
        };

        MockHttpServletResponse response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(createNodeCommand.nodeType, node.getNodeType());
        assertEquals(createNodeCommand.name, node.getName());
        assertEquals(createNodeCommand.contentUri, node.getContentUri());
    }

    @Test
    public void can_create_topic_with_id() throws Exception {
        final var createNodeCommand = new NodeCommand() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = "1";
                name = "trigonometry";
            }
        };

        testUtils.createResource("/v1/nodes", createNodeCommand);

        Node node = nodeRepository.getByPublicId(createNodeCommand.getPublicId());
        assertEquals(createNodeCommand.name, node.getName());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new NodeCommand() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = "1";
                name = "name";
            }
        };

        testUtils.createResource("/v1/nodes", command, status().isCreated());
        testUtils.createResource("/v1/nodes", command, status().isConflict());
    }

    @Test
    public void can_update_node() throws Exception {
        Node n = builder.node();

        testUtils.updateResource("/v1/nodes/" + n.getPublicId(), new NodeCommand() {
            {
                nodeType = n.getNodeType();
                nodeId = n.getIdent();
                name = "trigonometry";
                contentUri = URI.create("urn:article:1");
            }
        });

        Node node = nodeRepository.getByPublicId(n.getPublicId());
        assertEquals("trigonometry", node.getName());
        assertEquals("urn:article:1", node.getContentUri().toString());
    }

    @Test
    public void can_update_node_with_new_id() throws Exception {
        URI publicId = builder.node(NodeType.TOPIC).getPublicId();
        URI randomId = URI.create("urn:topic:random");

        testUtils.updateResource("/v1/nodes/" + publicId, new NodeCommand() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = "random";
                name = "trigonometry";
                contentUri = URI.create("urn:article:1");
            }
        });

        Node node = nodeRepository.getByPublicId(randomId);
        assertEquals("trigonometry", node.getName());
        assertEquals("urn:article:1", node.getContentUri().toString());
    }

    @Test
    public void can_update_node_with_new_type() throws Exception {
        Node n = builder.node(); // NODE
        String ident = n.getIdent();

        var command = new NodeCommand() {
            {
                nodeType = NodeType.SUBJECT;
                nodeId = ident;
                name = "trigonometry";
                contentUri = URI.create("urn:article:1");
            }
        };

        testUtils.updateResource("/v1/nodes/" + n.getPublicId(), command);

        Node node = nodeRepository.getByPublicId(command.getPublicId());
        assertEquals(NodeType.SUBJECT.getName() + ":" + ident, node.getPublicId().getSchemeSpecificPart());
        assertEquals("trigonometry", node.getName());
        assertEquals("urn:article:1", node.getContentUri().toString());
    }

    @Test
    public void can_update_node_without_changing_metadata() throws Exception {
        Node n = builder.node(s -> s.isVisible(false).grepCode("KM123").customField("key", "value"));
        String ident = n.getIdent();

        final var command = new NodeCommand() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = ident;
                name = "physics";
                contentUri = URI.create("urn:article:1");
            }
        };

        testUtils.updateResource("/v1/nodes/" + n.getPublicId(), command);

        Node node = nodeRepository.getByPublicId(command.getPublicId());
        assertEquals(command.nodeType, node.getNodeType());
        assertEquals(command.name, node.getName());
        assertEquals(command.contentUri, node.getContentUri());

        assertFalse(node.getMetadata().isVisible());
        assertTrue(node.getMetadata().getGrepCodes().stream().map(GrepCode::getCode).collect(Collectors.toSet())
                .contains("KM123"));
        assertTrue(node.getMetadata().getCustomFieldValues().stream().map(CustomFieldValue::getValue)
                .collect(Collectors.toSet()).contains("value"));
    }

    @Test
    public void can_delete_node_with_2_subnodes() throws Exception {
        Node childTopic1 = builder.node(NodeType.TOPIC, child -> child.name("DELETE EDGE TO ME"));
        Node childTopic2 = builder.node(NodeType.TOPIC, child -> child.name("DELETE EDGE TO ME ALSO"));

        URI parentId = builder.node(NodeType.TOPIC, parent -> parent.child(childTopic1).child(childTopic2))
                .getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
    }

    @Test
    public void can_delete_node_with_2_resources() throws Exception {
        Node topic = builder.node(NodeType.TOPIC,
                child -> child.name("MAIN TOPIC").translation("nb", tr -> tr.name("HovedEmne"))
                        .resource(r -> r.publicId("urn:resource:1")).resource(r -> r.publicId("urn:resource:2")));

        final var topicId = topic.getPublicId();

        testUtils.deleteResource("/v1/nodes/" + topicId);

        assertNull(nodeRepository.findByPublicId(topicId));
    }

    @Test
    public void can_delete_node_but_subnodes_remain() throws Exception {
        Node childTopic = builder.node(NodeType.TOPIC,
                child -> child.name("DELETE EDGE TO ME").translation("nb", tr -> tr.name("emne"))
                        .child(NodeType.TOPIC, sub -> sub.publicId("urn:topic:1"))
                        .resource(r -> r.publicId("urn:resource:1")));

        URI parentId = builder.node(NodeType.TOPIC, parent -> parent.child(childTopic)).getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
        assertNotNull(nodeRepository.findByPublicId(childTopic.getPublicId()));
    }

    @Test
    public void can_delete_nodes_but_resources_remain() throws Exception {
        Resource resource = builder.resource("resource",
                r -> r.translation("nb", tr -> tr.name("ressurs")).resourceType(rt -> rt.name("Learning path")));

        URI parentId = builder.node(NodeType.TOPIC, parent -> parent.resource(resource)).getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
        assertNotNull(resourceRepository.findByPublicId(resource.getPublicId()));
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
