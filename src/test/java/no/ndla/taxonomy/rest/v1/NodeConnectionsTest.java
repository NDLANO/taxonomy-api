/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NodeConnectionsTest extends RestTest {

    @Test
    public void can_add_child_to_parent() throws Exception {
        URI integrationId, calculusId;
        calculusId = builder.node(NodeType.TOPIC, t -> t.name("calculus")).getPublicId();
        integrationId = builder.node(NodeType.TOPIC, t -> t.name("integration")).getPublicId();

        URI id = getId(testUtils.createResource("/v1/node-connections", new NodeConnections.AddChildToParentCommand() {
            {
                parentId = calculusId;
                childId = integrationId;
            }
        }));

        final var connection = nodeConnectionRepository.findByPublicId(id);

        Node calculus = nodeRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getChildNodes().size());
        assertAnyTrue(calculus.getChildNodes(), t -> "integration".equals(t.getName()));
        assertNotNull(nodeConnectionRepository.getByPublicId(id));
    }

    @Test
    public void cannot_add_existing_child_to_parent() throws Exception {
        URI integrationId = builder.node("integration", NodeType.TOPIC, t -> t.name("integration")).getPublicId();
        URI calculusId = builder.node(NodeType.TOPIC, t -> t.name("calculus").child("integration")).getPublicId();

        testUtils.createResource("/v1/node-connections", new NodeConnections.AddChildToParentCommand() {
            {
                parentId = calculusId;
                childId = integrationId;
            }
        }, status().isConflict());
    }

    @Test
    public void cannot_add_root_child_to_parent() throws Exception {
        URI integrationId = builder.node("integration", NodeType.TOPIC, t -> t.isRoot(true).name("integration"))
                .getPublicId();
        URI calculusId = builder.node(NodeType.TOPIC, t -> t.name("calculus")).getPublicId();

        testUtils.createResource("/v1/node-connections", new NodeConnections.AddChildToParentCommand() {
            {
                parentId = calculusId;
                childId = integrationId;
            }
        }, status().isBadRequest());
    }

    @Test
    public void can_delete_parent_child() throws Exception {
        URI id = save(NodeConnection.create(newTopic(), newTopic())).getPublicId();
        testUtils.deleteResource("/v1/node-connections/" + id);
        assertNull(nodeRepository.findByPublicId(id));
    }

    @Test
    public void can_get_nodes() throws Exception {
        URI alternatingCurrentId = builder.node("ac", NodeType.TOPIC, t -> t.name("alternating current")).getPublicId();
        URI electricityId = builder.node(NodeType.TOPIC, t -> t.name("electricity").child("ac", NodeType.TOPIC))
                .getPublicId();
        URI integrationId = builder.node("integration", NodeType.TOPIC, t -> t.name("integration")).getPublicId();
        URI calculusId = builder.node(NodeType.TOPIC, t -> t.name("calculus").child("integration", NodeType.TOPIC))
                .getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/node-connections");
        NodeConnections.ParentChildIndexDocument[] parentChildren = testUtils
                .getObject(NodeConnections.ParentChildIndexDocument[].class, response);

        assertEquals(2, parentChildren.length);
        assertAnyTrue(parentChildren, t -> electricityId.equals(t.parentId) && alternatingCurrentId.equals(t.childId));
        assertAnyTrue(parentChildren, t -> calculusId.equals(t.parentId) && integrationId.equals(t.childId));
        assertAllTrue(parentChildren, t -> isValidId(t.id));
    }

    @Test
    public void can_get_node_connections_paginated() throws Exception {
        List<NodeConnection> connections = createTenContiguousRankedConnections();

        MockHttpServletResponse response = testUtils.getResource("/v1/node-connections?page=0&pageSize=5");
        NodeConnections.ParentChildIndexDocument[] parentChildren = testUtils
                .getObject(NodeConnections.ParentChildIndexDocument[].class, response);
        assertEquals(5, parentChildren.length);

        MockHttpServletResponse response2 = testUtils.getResource("/v1/node-connections?page=1&pageSize=5");
        NodeConnections.ParentChildIndexDocument[] parentChildren2 = testUtils
                .getObject(NodeConnections.ParentChildIndexDocument[].class, response2);
        assertEquals(5, parentChildren2.length);

        var results = new NodeConnections.ParentChildIndexDocument[parentChildren.length + parentChildren2.length];
        System.arraycopy(parentChildren, 0, results, 0, parentChildren.length);
        System.arraycopy(parentChildren, 0, results, parentChildren.length, parentChildren2.length);

        assertAllTrue(results,
                t -> connections.stream().map(DomainEntity::getPublicId).collect(Collectors.toList()).contains(t.id));
    }

    @Test
    public void can_get_node_child() throws Exception {
        URI topicid, subtopicid, id;
        Node electricity = newTopic().name("electricity");
        Node alternatingCurrent = newTopic().name("alternating current");
        NodeConnection topicSubtopic = save(NodeConnection.create(electricity, alternatingCurrent));

        topicid = electricity.getPublicId();
        subtopicid = alternatingCurrent.getPublicId();
        id = topicSubtopic.getPublicId();

        MockHttpServletResponse resource = testUtils.getResource("/v1/node-connections/" + id);
        NodeConnections.ParentChildIndexDocument parentChildIndexDocument = testUtils
                .getObject(NodeConnections.ParentChildIndexDocument.class, resource);

        assertEquals(topicid, parentChildIndexDocument.parentId);
        assertEquals(subtopicid, parentChildIndexDocument.childId);
    }

    @Test
    public void children_have_default_rank() throws Exception {
        builder.node(NodeType.TOPIC,
                t -> t.name("electricity").child(NodeType.TOPIC, st -> st.name("alternating currents"))
                        .child(NodeType.TOPIC, st -> st.name("wiring")));
        MockHttpServletResponse response = testUtils.getResource(("/v1/node-connections"));
        NodeConnections.ParentChildIndexDocument[] children = testUtils
                .getObject(NodeConnections.ParentChildIndexDocument[].class, response);

        assertAllTrue(children, st -> st.rank == 0);
    }

    @Test
    public void chlidren_can_be_created_with_rank() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Subject").publicId("urn:subject:1"));
        Node electricity = builder.node(NodeType.TOPIC, s -> s.name("Electricity").publicId("urn:topic:1"));
        save(NodeConnection.create(subject, electricity));
        Node alternatingCurrents = builder.node(NodeType.TOPIC,
                t -> t.name("Alternating currents").publicId("urn:topic:11"));
        Node wiring = builder.node(NodeType.TOPIC, t -> t.name("Wiring").publicId("urn:topic:12"));

        testUtils.createResource("/v1/node-connections", new NodeConnections.AddChildToParentCommand() {
            {
                parentId = electricity.getPublicId();
                childId = alternatingCurrents.getPublicId();
                rank = 2;
            }
        });

        testUtils.createResource("/v1/node-connections", new NodeConnections.AddChildToParentCommand() {
            {
                parentId = electricity.getPublicId();
                childId = wiring.getPublicId();
                rank = 1;
            }
        });

        MockHttpServletResponse response = testUtils
                .getResource("/v1/subjects/" + subject.getPublicId() + "/topics?recursive=true");
        TopicSubtopics.TopicSubtopicIndexDocument[] topics = testUtils
                .getObject(TopicSubtopics.TopicSubtopicIndexDocument[].class, response);

        assertEquals(electricity.getPublicId(), topics[0].id);
        assertEquals(wiring.getPublicId(), topics[1].id);
        assertEquals(alternatingCurrents.getPublicId(), topics[2].id);
    }

    @Test
    public void can_update_child_rank() throws Exception {
        URI id = save(NodeConnection.create(newTopic(), newTopic())).getPublicId();
        testUtils.updateResource("/v1/node-connections/" + id, new NodeConnections.UpdateNodeChildCommand() {
            {
                primary = true;
                rank = 99;
            }
        });
        assertEquals(99, nodeConnectionRepository.getByPublicId(id).getRank());
    }

    @Test
    public void update_child_rank_modifies_other_contiguous_ranks() throws Exception {
        List<NodeConnection> nodeConnections = createTenContiguousRankedConnections(); // creates ranks 1, 2, 3, 4, 5,
                                                                                       // 6, 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeConnections);

        // make the last object the first
        NodeConnection updatedConnection = nodeConnections.get(nodeConnections.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-connections/" + updatedConnection.getPublicId().toString(),
                new NodeConnections.UpdateNodeChildCommand() {
                    {
                        primary = true;
                        rank = 1;
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (NodeConnection nodeConnection : nodeConnections) {
            MockHttpServletResponse response = testUtils
                    .getResource("/v1/node-connections/" + nodeConnection.getPublicId().toString());
            NodeConnections.ParentChildIndexDocument connectionFromDb = testUtils
                    .getObject(NodeConnections.ParentChildIndexDocument.class, response);
            // verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_child_rank_does_not_alter_noncontiguous_ranks() throws Exception {

        List<NodeConnection> nodeConnections = createTenNonContiguousRankedConnections(); // creates ranks 1, 2, 3, 4,
                                                                                          // 5, 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeConnections);

        // make the last object the first
        NodeConnection updatedConnection = nodeConnections.get(nodeConnections.size() - 1);
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-connections/" + updatedConnection.getPublicId().toString(),
                new NodeConnections.UpdateNodeChildCommand() {
                    {
                        primary = true;
                        rank = 1;
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (NodeConnection nodeConnection : nodeConnections) {
            MockHttpServletResponse response = testUtils
                    .getResource("/v1/node-connections/" + nodeConnection.getPublicId().toString());
            NodeConnections.ParentChildIndexDocument connectionFromDb = testUtils
                    .getObject(NodeConnections.ParentChildIndexDocument.class, response);
            // verify that only the contiguous connections are updated
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                if (oldRank <= 5) {
                    assertEquals(oldRank + 1, connectionFromDb.rank);
                } else {
                    assertEquals(oldRank, connectionFromDb.rank);
                }
            }
        }
    }

    @Test
    public void update_child_rank_higher_rank_does_not_modify_existing_connections() throws Exception {
        List<NodeConnection> nodeConnections = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeConnections);

        // set rank for last object to higher than any existing
        NodeConnection updatedConnection = nodeConnections.get(nodeConnections.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-connections/" + nodeConnections.get(9).getPublicId().toString(),
                new NodeConnections.UpdateNodeChildCommand() {
                    {
                        primary = true;
                        rank = 99;
                    }
                });
        assertEquals(99, updatedConnection.getRank());

        // verify that the other connections are unchanged
        for (NodeConnection nodeConnection : nodeConnections) {
            MockHttpServletResponse response = testUtils
                    .getResource("/v1/node-connections/" + nodeConnection.getPublicId().toString());
            NodeConnections.ParentChildIndexDocument connection = testUtils
                    .getObject(NodeConnections.ParentChildIndexDocument.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    @Test
    public void update_metadata_for_connection() throws Exception {
        URI id = save(NodeConnection.create(newTopic(), newTopic())).getPublicId();
        testUtils.updateResource("/v1/node-connections/" + id + "/metadata", new MetadataDto() {
            {
                visible = false;
                grepCodes = Set.of("KM123");
                customFields = Map.of("key", "value");
            }
        }, status().isOk());
        NodeConnection connection = nodeConnectionRepository.getByPublicId(id);
        assertFalse(connection.getMetadata().isVisible());
        Set<String> codes = connection.getMetadata().getGrepCodes().stream().map(GrepCode::getCode)
                .collect(Collectors.toSet());
        assertTrue(codes.contains("KM123"));
        Set<CustomFieldValue> customFieldValues = connection.getMetadata().getCustomFieldValues();
        assertTrue(customFieldValues.stream().map(CustomFieldValue::getCustomField).map(CustomField::getKey)
                .collect(Collectors.toSet()).contains("key"));
        assertTrue(customFieldValues.stream().map(CustomFieldValue::getValue).collect(Collectors.toSet())
                .contains("value"));

    }

    private Map<String, Integer> mapConnectionRanks(List<NodeConnection> nodeConnections) {
        Map<String, Integer> mappedRanks = new HashMap<>();
        for (NodeConnection nc : nodeConnections) {
            mappedRanks.put(nc.getPublicId().toString(), nc.getRank());
        }
        return mappedRanks;
    }

    private List<NodeConnection> createTenContiguousRankedConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Node sub = newTopic();
            NodeConnection nodeConnection = NodeConnection.create(parent, sub);
            nodeConnection.setRank(i);
            connections.add(nodeConnection);
            save(nodeConnection);
        }
        return connections;
    }

    private List<NodeConnection> createTenNonContiguousRankedConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Node sub = newTopic();
            NodeConnection nodeConnection = NodeConnection.create(parent, sub);
            if (i <= 5) {
                nodeConnection.setRank(i);
            } else {
                nodeConnection.setRank(i * 10);
            }
            connections.add(nodeConnection);
            save(nodeConnection);
        }
        return connections;
    }
}
