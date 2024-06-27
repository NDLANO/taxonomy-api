/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.commands.NodePostPut;
import no.ndla.taxonomy.service.dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class NodesTest extends RestTest {
    @Autowired
    EntityManager entityManager;

    @Autowired
    private TestSeeder testSeeder;

    @Value(value = "${new.url.separator:false}")
    private boolean newUrlSeparator;

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
        resourceTypeRepository.deleteAllAndFlush();
        resourceResourceTypeRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_single_node() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("maths").publicId("urn:subject:1").child(t -> t.nodeType(NodeType.TOPIC)
                        .name("trigonometry")
                        .contentUri("urn:article:1")
                        .publicId("urn:topic:1")));

        var response = testUtils.getResource("/v1/nodes/urn:topic:1");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("trigonometry", node.getName());
        assertEquals("Optional[urn:article:1]", node.getContentUri().toString());
        assertEquals("/subject:1/topic:1", node.getPath());
        assertEquals(List.of("maths", "trigonometry"), node.getBreadcrumbs());

        assertNotNull(node.getMetadata());
        assertTrue(node.getMetadata().isVisible());
    }

    @Test
    public void single_node_has_no_url() throws Exception {
        builder.node(NodeType.NODE, t -> t.publicId("urn:node:1"));

        var response = testUtils.getResource("/v1/nodes/urn:node:1");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("", node.getPath());
    }

    @Test
    public void single_context_node_has_url() throws Exception {
        builder.node(NodeType.NODE, t -> t.isContext(true).publicId("urn:node:1"));

        var response = testUtils.getResource("/v1/nodes/urn:node:1");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("/node:1", node.getPath());
    }

    @Test
    public void can_get_nodes_by_contentURI() throws Exception {
        builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> {
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
            assertEquals(List.of("Basic science", "photo synthesis"), nodes[0].getBreadcrumbs());
        }

        {
            final var response = testUtils.getResource("/v1/nodes?contentURI=urn:test:2");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("trigonometry", nodes[0].getName());
            assertEquals(List.of("Maths", "trigonometry"), nodes[0].getBreadcrumbs());
        }
    }

    @Test
    public void can_get_nodes_by_contextId() {
        Node resource = builder.node(NodeType.RESOURCE, r -> r.name("Resource"));
        builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> {
                    t.name("photo synthesis");
                    t.contentUri(URI.create("urn:test:1")).resource(resource);
                }));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> {
            t.name("trigonometry");
            t.contentUri(URI.create("urn:test:2")).resource(resource);
        }));

        Optional<Node> fromDB = nodeRepository.findFirstByPublicId(resource.getPublicId());
        assertTrue(fromDB.isPresent());
        assertEquals(2, fromDB.get().getContexts().size());

        // Check fetching by contextid and make sure correct path and breadcrumbs is used
        fromDB.get().getContexts().forEach(context -> {
            try {
                final var response =
                        testUtils.getResource("/v1/nodes?nodeType=RESOURCE&contextId=" + context.contextId());
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(1, nodes.length);
                assertEquals("Resource", nodes[0].getName());
                if (newUrlSeparator) {
                    assertTrue(nodes[0].getUrl().get().endsWith(String.format("/resource/r/%s", context.contextId())));
                } else {
                    assertTrue(nodes[0].getUrl().get().endsWith(String.format("/resource__%s", context.contextId())));
                }
                assertEquals(context.path(), nodes[0].getPath());
                assertTrue(nodes[0].getBreadcrumbs()
                        .containsAll(context.breadcrumbs().get("nb")));
            } catch (Exception e) {
                // Not happening
            }
        });
    }

    @Test
    public void can_get_nodes_by_key_and_value() throws Exception {
        builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(t -> {
                    t.nodeType(NodeType.TOPIC);
                    t.name("photo synthesis");
                    t.contentUri(URI.create("urn:test:1"));
                    t.grepCode("GREP1");
                    t.customField("test", "value");
                }));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> {
            t.name("trigonometry");
            t.contentUri(URI.create("urn:test:2"));
            t.grepCode("GREP2");
            t.customField("test", "value2");
        }));

        {
            final var response = testUtils.getResource("/v1/nodes?value=value");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("photo synthesis", nodes[0].getName());
            assertNotNull(nodes[0].getMetadata());
            assertNotNull(nodes[0].getMetadata().getGrepCodes());
            assertEquals(Set.of("GREP1"), nodes[0].getMetadata().getGrepCodes());
        }
        {
            final var response = testUtils.getResource("/v1/nodes?key=test&value=value");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("photo synthesis", nodes[0].getName());
            assertNotNull(nodes[0].getMetadata());
            assertNotNull(nodes[0].getMetadata().getGrepCodes());
            assertEquals(Set.of("GREP1"), nodes[0].getMetadata().getGrepCodes());
        }
        {
            final var response = testUtils.getResource("/v1/nodes?key=test&value=value2");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("trigonometry", nodes[0].getName());
            assertNotNull(nodes[0].getMetadata());
            assertNotNull(nodes[0].getMetadata().getGrepCodes());
            assertEquals(Set.of("GREP2"), nodes[0].getMetadata().getGrepCodes());
        }
    }

    @Test
    public void can_get_all_nodes() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> t.name("trigonometry")));

        var response = testUtils.getResource("/v1/nodes");
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
        assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_get_nodes_paginated() throws Exception {
        var node1 = builder.node(NodeType.NODE);
        var node2 = builder.node(NodeType.NODE);

        var response = testUtils.getResource("/v1/nodes/page?nodeType=NODE&page=1&pageSize=1");
        var page1 = testUtils.getObject(SearchResultDTO.class, response);
        assertEquals(1, page1.getResults().size());

        var response2 = testUtils.getResource("/v1/nodes/page?nodeType=NODE&page=2&pageSize=1");
        var page2 = testUtils.getObject(SearchResultDTO.class, response2);
        assertEquals(1, page2.getResults().size());

        var result = Stream.concat(page1.getResults().stream(), page2.getResults().stream())
                .toList();

        // noinspection SuspiciousMethodCalls
        assertTrue(Stream.of(node1, node2)
                .map(DomainEntity::getPublicId)
                .map(Object::toString)
                .toList()
                .containsAll(result.stream()
                        .map(r -> ((LinkedHashMap<String, String>) r).get("id"))
                        .toList()));
    }

    @Test
    public void can_get_all_root_nodes() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> t.name("trigonometry")));
        builder.node(NodeType.SUBJECT, s -> s.name("Arts and crafts"));
        builder.node(
                NodeType.NODE, n -> n.isContext(true).name("Random node").child(NodeType.NODE, c -> c.name("Subnode")));

        {
            {
                var response = testUtils.getResource("/v1/nodes?isRoot=true");
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(3, nodes.length);
                assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            }
        }
        {
            {
                var response = testUtils.getResource("/v1/nodes?isContext=true");
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(3, nodes.length);
                assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            }
        }
        {
            {
                var response = testUtils.getResource("/v1/nodes?isContext=false");
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(4, nodes.length);
                assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
                assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Arts and crafts".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Subnode".equals(t.getName()));
            }
        }
        {
            {
                var response = testUtils.getResource("/v1/nodes?isContext=true&nodeType=SUBJECT");
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(2, nodes.length);
                assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
            }
        }
        {
            var response = testUtils.getResource("/v1/nodes?isContext=true");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(3, nodes.length);

            assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().contains("subject"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));

            assertAllTrue(nodes, t -> t.getMetadata() != null);
            assertAllTrue(nodes, t -> t.getMetadata().isVisible());
            assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
        }
    }

    @Test
    public void can_get_sub_nodes() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("Basic science")
                .child(NodeType.TOPIC, t -> t.name("photo synthesis").resource(r -> r.name("mithocondria")))
                .child(NodeType.TOPIC, t -> t.name("trigonometry").resource(r -> r.name("angles")))
                .child(NodeType.TOPIC, t -> t.name("Random node").child(NodeType.NODE, c -> c.name("Subnode"))));
        {
            var response = testUtils.getResource(
                    "/v1/nodes/" + subject.getPublicId() + "/nodes?recursive=true&nodeType=TOPIC,RESOURCE,NODE");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(6, nodes.length);

            assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
            assertAnyTrue(nodes, t -> "mithocondria".equals(t.getName()));
            assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
            assertAnyTrue(nodes, t -> "angles".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Subnode".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().contains("subject"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));

            assertAllTrue(nodes, t -> t.getMetadata() != null);
            assertAllTrue(nodes, t -> t.getMetadata().isVisible());
            assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
        }
        {
            var response = testUtils.getResource(
                    "/v1/nodes/" + subject.getPublicId() + "/nodes?recursive=true&nodeType=TOPIC,RESOURCE");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(5, nodes.length);

            assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
            assertAnyTrue(nodes, t -> "mithocondria".equals(t.getName()));
            assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
            assertAnyTrue(nodes, t -> "angles".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().contains("subject"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));

            assertAllTrue(nodes, t -> t.getMetadata() != null);
            assertAllTrue(nodes, t -> t.getMetadata().isVisible());
            assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
        }
        {
            var response = testUtils.getResource(
                    "/v1/nodes/" + subject.getPublicId() + "/nodes?recursive=true&nodeType=TOPIC");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(3, nodes.length);

            assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
            assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().contains("subject"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));

            assertAllTrue(nodes, t -> t.getMetadata() != null);
            assertAllTrue(nodes, t -> t.getMetadata().isVisible());
            assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
        }
    }

    @Test
    public void can_filter_nodes() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("Maths")
                .isVisible(false)
                .child(NodeType.TOPIC, t -> t.name("trigonometry")));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Arts and crafts"));
        builder.node(
                NodeType.NODE, n -> n.isContext(true).name("Random node").child(NodeType.NODE, c -> c.name("Subnode")
                        .contentUri("urn:article:1")
                        .isVisible(false)));

        {
            var response = testUtils.getResource("/v1/nodes?contentURI=urn:article:1");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertAnyTrue(nodes, t -> "Subnode".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().contains("node"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));
        }
        {
            var response = testUtils.getResource("/v1/nodes?isVisible=true");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(5, nodes.length);
            assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
            assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
            assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Arts and crafts".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
        }
    }

    @Test
    public void can_filter_nodes_on_ids() throws Exception {
        var node1 = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("Basic science")
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        var node2 = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("Maths")
                .isVisible(false)
                .child(NodeType.TOPIC, t -> t.name("trigonometry")));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Arts and crafts"));
        builder.node(
                NodeType.NODE, n -> n.isContext(true).name("Random node").child(NodeType.NODE, c -> c.name("Subnode")
                        .contentUri("urn:article:1")
                        .isVisible(false)));

        {
            var response = testUtils.getResource("/v1/nodes?ids=" + node1.getPublicId() + "," + node2.getPublicId());
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(2, nodes.length);
            assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
        }
    }

    @Test
    void fetching_subnodes_uses_correct_context() throws Exception {
        Node resource = builder.node(NodeType.RESOURCE, r -> r.name("Leaf").publicId("urn:resource:1"));
        builder.node(NodeType.SUBJECT, s1 -> s1.isContext(true)
                .name("Subject1")
                .publicId("urn:subject:1")
                .child(
                        NodeType.TOPIC,
                        t1 -> t1.name("Topic1").publicId("urn:topic:1").resource(resource)));
        builder.node(NodeType.SUBJECT, s2 -> s2.isContext(true)
                .name("Subject2")
                .publicId("urn:subject:2")
                .child(NodeType.TOPIC, t2 -> t2.name("Topic2")
                        .publicId("urn:topic:2")
                        .isContext(true)
                        .child(
                                NodeType.TOPIC,
                                t3 -> t3.name("Topic3").publicId("urn:topic:3").resource(resource))));

        {
            var response =
                    testUtils.getResource("/v1/nodes/urn:subject:1/nodes?nodeType=TOPIC,RESOURCE&recursive=true");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(2, nodes.length);
            assertAllTrue(nodes, t -> t.getPath().contains("/subject:1/topic:1"));
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes?nodeType=RESOURCE");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("Leaf", nodes[0].getName());
            assertEquals(3, nodes[0].getBreadcrumbs().size());
            assertEquals("/subject:1/topic:1/resource:1", nodes[0].getPath());
        }
        {
            var response =
                    testUtils.getResource("/v1/nodes/urn:subject:2/nodes?nodeType=TOPIC,RESOURCE&recursive=true");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(3, nodes.length);
            assertAllTrue(nodes, t -> t.getPath().contains("/subject:2/topic:2"));
        }
        {
            // Topic2 is context
            var response = testUtils.getResource("/v1/nodes/urn:topic:2/nodes?nodeType=TOPIC,RESOURCE&recursive=true");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(2, nodes.length);
            assertAllTrue(nodes, t -> t.getPath().contains("/topic:2/topic:3"));
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:topic:3/nodes?nodeType=RESOURCE");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("Leaf", nodes[0].getName());
            // Can't predict which of the two contexts with topic:3 that is picked
            assertAllTrue(nodes, t -> t.getPaths().contains("/topic:2/topic:3/resource:1"));
        }
    }

    @Test
    public void can_place_subject_below_subject() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Maths").publicId("urn:subject:1").child(NodeType.SUBJECT, t -> t.name(
                                "Maths vg1")
                        .contentUri("urn:frontpage:1")
                        .publicId("urn:subject:2")));

        var response = testUtils.getResource("/v1/nodes/urn:subject:2");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("Maths vg1", node.getName());
        assertEquals("Optional[urn:frontpage:1]", node.getContentUri().toString());
        assertEquals("/subject:1/subject:2", node.getPath());

        assertNotNull(node.getMetadata());
        assertTrue(node.getMetadata().isVisible());
        assertEquals(0, node.getMetadata().getGrepCodes().size());
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

        var response = testUtils.getResource("/v1/nodes/urn:topic:2000/connections");
        final var connections = testUtils.getObject(ConnectionDTO[].class, response);

        assertEquals(3, connections.length, "Correct number of connections");
        assertAllTrue(connections, c -> !c.getPaths().isEmpty()); // all connections have at least one path

        connectionsHaveCorrectTypes(connections);
    }

    private void connectionsHaveCorrectTypes(ConnectionDTO[] connections) {
        ConnectionTypeCounter connectionTypeCounter = new ConnectionTypeCounter(connections).countTypes();
        assertEquals(0, connectionTypeCounter.getSubjectCount());
        assertEquals(1, connectionTypeCounter.getParentCount());
        assertEquals(2, connectionTypeCounter.getChildCount());
    }

    @Test
    public void subnodes_are_sorted_by_rank() throws Exception {
        testSeeder.subtopicsByNodeIdAndRelevanceTestSetup();

        var response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes");
        final var subtopics = testUtils.getObject(NodeChildDTO[].class, response);
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

        var response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes");
        final var subtopics = testUtils.getObject(NodeChildDTO[].class, response);
        assertEquals(7, subtopics.length, "Unfiltered subtopics");

        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata() != null);
        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata().isVisible());
        assertAllTrue(
                subtopics, subtopic -> subtopic.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_create_node() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.NODE;
                name = Optional.of("node");
                contentUri = Optional.of(URI.create("urn:article:1"));
                context = Optional.of(Boolean.TRUE);
            }
        };

        var response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(NodeType.NODE, node.getNodeType());
        assertEquals("node", node.getName());
        assertEquals(URI.create("urn:article:1"), node.getContentUri());
        assertTrue(node.isContext());
    }

    @Test
    public void can_create_invisible_node() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.NODE;
                name = Optional.of("node");
                contentUri = Optional.of(URI.create("urn:article:1"));
                context = Optional.of(Boolean.TRUE);
                visible = Optional.of(Boolean.FALSE);
            }
        };

        var response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(NodeType.NODE, node.getNodeType());
        assertEquals("node", node.getName());
        assertEquals(URI.create("urn:article:1"), node.getContentUri());
        assertTrue(node.isContext());
        assertFalse(node.isVisible());
    }

    @Test
    public void can_create_topic() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                name = Optional.of("trigonometry");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        var response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(NodeType.TOPIC, node.getNodeType());
        assertEquals("trigonometry", node.getName());
        assertEquals(URI.create("urn:article:1"), node.getContentUri());
    }

    @Test
    public void can_create_subject() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.SUBJECT;
                name = Optional.of("Maths");
                contentUri = Optional.of(URI.create("urn:frontpage:1"));
            }
        };

        var response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(NodeType.SUBJECT, node.getNodeType());
        assertEquals("Maths", node.getName());
        assertEquals(URI.create("urn:frontpage:1"), node.getContentUri());
    }

    @Test
    public void can_create_topic_with_id() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = Optional.of("1");
                name = Optional.of("trigonometry");
            }
        };

        testUtils.createResource("/v1/nodes", createNodeCommand);

        Node node = nodeRepository.getByPublicId(createNodeCommand.getPublicId());
        assertEquals("trigonometry", node.getName());
    }

    @Test
    void creating_node_with_content_uri_is_handled_correct() throws Exception {
        final var createTopic = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                name = Optional.of("topic");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };
        final var createResource = new NodePostPut() {
            {
                nodeType = NodeType.RESOURCE;
                name = Optional.of("resource");
                contentUri = Optional.of(URI.create("urn:article:2"));
            }
        };

        testUtils.createResource("/v1/nodes", createTopic, status().isCreated());
        testUtils.createResource("/v1/nodes", createTopic, status().isCreated());
        testUtils.createResource("/v1/nodes", createResource, status().isCreated());
        testUtils.createResource("/v1/nodes", createResource, status().isConflict());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = Optional.of("1");
                name = Optional.of("name");
            }
        };

        testUtils.createResource("/v1/nodes", command, status().isCreated());
        testUtils.createResource("/v1/nodes", command, status().isConflict());
    }

    @Test
    public void can_update_node() throws Exception {
        Node n = builder.node();

        testUtils.updateResource("/v1/nodes/" + n.getPublicId(), new NodePostPut() {
            {
                nodeType = n.getNodeType();
                nodeId = Optional.of(n.getIdent());
                name = Optional.of("trigonometry");
                contentUri = Optional.of(URI.create("urn:article:1"));
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

        testUtils.updateResource("/v1/nodes/" + publicId, new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = Optional.of("random");
                name = Optional.of("trigonometry");
                contentUri = Optional.of(URI.create("urn:article:1"));
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

        var command = new NodePostPut() {
            {
                nodeType = NodeType.SUBJECT;
                nodeId = Optional.of(ident);
                name = Optional.of("trigonometry");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        testUtils.updateResource("/v1/nodes/" + n.getPublicId(), command);

        Node node = nodeRepository.getByPublicId(command.getPublicId());
        assertEquals(
                NodeType.SUBJECT.getName() + ":" + ident, node.getPublicId().getSchemeSpecificPart());
        assertEquals("trigonometry", node.getName());
        assertEquals("urn:article:1", node.getContentUri().toString());
    }

    @Test
    public void can_update_node_without_changing_metadata() throws Exception {
        Node n = builder.node(s -> s.isVisible(false).grepCode("KM123").customField("key", "value"));

        final var command = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                name = Optional.of("physics");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        testUtils.updateResource("/v1/nodes/" + n.getPublicId(), command);

        Node node = nodeRepository.getByPublicId(n.getPublicId());
        assertEquals(NodeType.TOPIC, node.getNodeType());
        assertEquals("physics", node.getName());
        assertEquals(URI.create("urn:article:1"), node.getContentUri());

        assertFalse(node.getMetadata().isVisible());
        assertTrue(node.getMetadata().getGrepCodes().stream()
                .map(JsonGrepCode::getCode)
                .collect(Collectors.toSet())
                .contains("KM123"));
        assertTrue(node.getCustomFields().containsValue("value"));
    }

    @Test
    public void can_delete_node_with_2_subnodes() throws Exception {
        Node childTopic1 = builder.node(NodeType.TOPIC, child -> child.name("DELETE EDGE TO ME"));
        Node childTopic2 = builder.node(NodeType.TOPIC, child -> child.name("DELETE EDGE TO ME ALSO"));

        URI parentId = builder.node(
                        NodeType.TOPIC, parent -> parent.child(childTopic1).child(childTopic2))
                .getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
    }

    @Test
    public void can_delete_node_with_2_resources() throws Exception {
        Node topic = builder.node(NodeType.TOPIC, child -> child.name("MAIN TOPIC")
                .translation("nb", tr -> tr.name("HovedEmne"))
                .resource(r -> r.publicId("urn:resource:1"))
                .resource(r -> r.publicId("urn:resource:2")));

        final var topicId = topic.getPublicId();

        testUtils.deleteResource("/v1/nodes/" + topicId);

        assertNull(nodeRepository.findByPublicId(topicId));
    }

    @Test
    public void can_delete_node_but_subnodes_remain() throws Exception {
        Node childTopic = builder.node(NodeType.TOPIC, child -> child.name("DELETE EDGE TO ME")
                .translation("nb", tr -> tr.name("emne"))
                .child(NodeType.TOPIC, sub -> sub.publicId("urn:topic:1"))
                .resource(r -> r.publicId("urn:resource:1")));

        URI parentId =
                builder.node(NodeType.TOPIC, parent -> parent.child(childTopic)).getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
        assertNotNull(nodeRepository.findByPublicId(childTopic.getPublicId()));
    }

    @Test
    public void can_delete_nodes_but_resources_remain() throws Exception {
        var x = builder.resourceType("rt", rt -> rt.name("Learning path"));
        Node resource = builder.node(NodeType.RESOURCE, r -> r.translation("nb", tr -> tr.name("ressurs")));

        var hallo = resource.addResourceType(x);
        entityManager.persist(hallo);

        URI parentId = builder.node(NodeType.TOPIC, parent -> parent.resource(resource))
                .getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
        assertNotNull(nodeRepository.findByPublicId(resource.getPublicId()));
    }

    @Test
    void publishing_node_fails_if_no_target_version() throws Exception {
        Node node = builder.node();
        var response = testUtils.updateResource(
                "/v1/nodes/" + node.getPublicId() + "/publish", null, status().is4xxClientError());
        assertEquals(400, response.getStatus());
    }

    @Test
    void publishing_node_fails_if_node_not_found() throws Exception {
        var response = testUtils.updateResource("/v1/nodes/urn:node:random/publish", null, status().is4xxClientError());
        assertEquals(400, response.getStatus());
    }

    @Test
    void making_resources_primary_sets_other_contexts_not_primary() throws Exception {
        var resource = builder.node(NodeType.RESOURCE);
        var resource2 = builder.node(NodeType.RESOURCE);
        var node1 = builder.node(NodeType.TOPIC, n -> n.resource(resource, false)
                .child(NodeType.TOPIC, c -> c.publicId("urn:topic:1").resource(resource2, false)));
        var node2 = builder.node(NodeType.TOPIC, n -> n.resource(resource, true)
                .child(NodeType.TOPIC, c -> c.publicId("urn:topic:2").resource(resource2, true)));

        {
            // Only set primary on one level
            var response = testUtils.updateResource(
                    "/v1/nodes/" + node1.getPublicId() + "/makeResourcesPrimary", null, status().isOk());
            assertEquals(200, response.getStatus());
            var updated1 = nodeRepository.getByPublicId(node1.getPublicId());
            assertTrue(updated1.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            var updated2 = nodeRepository.getByPublicId(node2.getPublicId());
            assertFalse(updated2.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            // Following should be unchanged
            var updated3 = nodeRepository.getByPublicId(URI.create("urn:topic:1"));
            assertFalse(updated3.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            var updated4 = nodeRepository.getByPublicId(URI.create("urn:topic:2"));
            assertTrue(updated4.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
        }
        {
            // Recursive flag updates all levels
            var response = testUtils.updateResource(
                    "/v1/nodes/" + node1.getPublicId() + "/makeResourcesPrimary?recursive=true", null, status().isOk());
            assertEquals(200, response.getStatus());
            var updated1 = nodeRepository.getByPublicId(node1.getPublicId());
            assertTrue(updated1.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            var updated2 = nodeRepository.getByPublicId(node2.getPublicId());
            assertFalse(updated2.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            // Switched order from previous block
            var updated3 = nodeRepository.getByPublicId(URI.create("urn:topic:1"));
            assertTrue(updated3.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            var updated4 = nodeRepository.getByPublicId(URI.create("urn:topic:2"));
            assertFalse(updated4.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
        }
    }

    @Test
    public void full_node_has_parents() throws Exception {
        testSeeder.resourceInDualSubjectsTestSetup();
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1/full");
            final var result = testUtils.getObject(NodeWithParents.class, response);
            assertNotNull(result.getPaths());
            assertEquals(2, result.getParents().size());
            assertEquals(0, result.getContexts().size());
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1/full?includeContexts=true");
            final var result = testUtils.getObject(NodeWithParents.class, response);
            assertNotNull(result.getPaths());
            assertEquals(2, result.getParents().size());
            assertEquals(2, result.getContexts().size());
        }
    }

    @Test
    public void can_clone_node() throws Exception {
        URI publicId = builder.node(NodeType.RESOURCE, r -> r.name("Resource")
                        .resourceType(rt -> rt.name("Fagstoff"))
                        .translation("nb", tr -> tr.name("Fagstoff nb"))
                        .contentUri("urn:article:1"))
                .getPublicId();

        final var command = new NodePostPut() {
            {
                contentUri = Optional.of(URI.create("urn:article:2"));
            }
        };
        URI id = getId(testUtils.createResource("/v1/nodes/" + publicId + "/clone", command));
        assertNotNull(id);

        var oldRes = nodeRepository.getByPublicId(publicId);
        var newRes = nodeRepository.getByPublicId(id);
        assertNotEquals(publicId, id);
        assertEquals(oldRes.getName(), newRes.getName());
        assertEquals("urn:article:1", oldRes.getContentUri().toString());
        assertEquals("urn:article:2", newRes.getContentUri().toString());
        assertNotEquals(
                oldRes.getContentUri().toString(), newRes.getContentUri().toString());
        assertEquals(oldRes.getTranslations().size(), newRes.getTranslations().size());
        assertEquals(oldRes.getResourceTypes().size(), newRes.getResourceTypes().size());

        // contentUri can be null
        URI id2 = getId(testUtils.createResource("/v1/nodes/" + publicId + "/clone", new NodePostPut()));
        assertNotNull(id2);
        var resWithoutContentUri = nodeRepository.findByPublicId(id2);
        assertNull(resWithoutContentUri.getContentUri());
    }

    @Test
    public void can_update_delete_and_leave_quality_evaluation() throws Exception {
        var node = builder.node(NodeType.TOPIC);
        var publicId = node.getPublicId();

        testUtils.updateResourceRawInput(
                "/v1/nodes/" + publicId,
                "{\"nodeType\":\"TOPIC\",\"name\":\"some-name\",\"contentUri\":\"urn:article:1\",\"qualityEvaluation\": {\"grade\": 5}}");
        var found = nodeRepository.getByPublicId(publicId);
        assertEquals(found.getQualityEvaluationGrade(), Optional.of(Grade.Five));

        testUtils.updateResourceRawInput(
                "/v1/nodes/" + publicId,
                "{\"nodeType\":\"TOPIC\",\"name\":\"some-name\",\"contentUri\":\"urn:article:1\"}");
        var found2 = nodeRepository.getByPublicId(publicId);
        assertEquals(found2.getQualityEvaluationGrade(), Optional.of(Grade.Five));

        testUtils.updateResourceRawInput(
                "/v1/nodes/" + publicId,
                "{\"nodeType\":\"TOPIC\",\"name\":\"some-name\",\"contentUri\":\"urn:article:1\",\"qualityEvaluation\": null}");
        var found3 = nodeRepository.getByPublicId(publicId);
        assertEquals(found3.getQualityEvaluationGrade(), Optional.empty());
    }

    @Test
    public void update_and_deletion_of_quality_evaluation_does_not_break_calculation() throws Exception {
        var subjectId = "urn:subject:1";
        var topicId = "urn:topic:1";
        builder.node(NodeType.SUBJECT, n -> n.name("S1").publicId(subjectId).child(NodeType.TOPIC, c -> c.name("T1")
                .publicId(topicId)));

        { // Set quality evaluation on topic and check that average on subject is updated
            testUtils.updateResourceRawInput("/v1/nodes/" + topicId, "{\"qualityEvaluation\": {\"grade\": 5}}");
            var dbTopic = nodeRepository.getByPublicId(URI.create(topicId));
            var dbSuject = nodeRepository.getByPublicId(URI.create(subjectId));
            assertEquals(dbTopic.getQualityEvaluationGrade(), Optional.of(Grade.Five));
            var expectedFive = Optional.of(new GradeAverage(5.0, 1));
            assertEquals(dbSuject.getChildQualityEvaluationAverage(), expectedFive);
        }

        { // Remove quality evaluation on topic and check that average on subject is removed
            testUtils.updateResourceRawInput("/v1/nodes/" + topicId, "{\"qualityEvaluation\": null}");
            var dbTopic = nodeRepository.getByPublicId(URI.create(topicId));
            var dbSuject = nodeRepository.getByPublicId(URI.create(subjectId));
            assertEquals(dbTopic.getQualityEvaluationGrade(), Optional.empty());
            var actual = dbSuject.getChildQualityEvaluationAverage();
            assertEquals(actual, Optional.empty());
        }
    }

    private static class ConnectionTypeCounter {
        private final ConnectionDTO[] connections;
        private int subjectCount;
        private int parentCount;
        private int childCount;

        ConnectionTypeCounter(ConnectionDTO[] connections) {
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
            for (ConnectionDTO connection : connections) {
                switch (connection.getType()) {
                    case "parent-subject" -> subjectCount++;
                    case "parent" -> parentCount++;
                    case "child" -> childCount++;
                    default -> fail("Unexpected connection type :" + connection.getType());
                }
            }
            return this;
        }
    }
}
