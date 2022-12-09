/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.ResourceWithNodeConnectionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NodeResourcesTest extends RestTest {

    @Test
    public void can_add_resource_to_node() throws Exception {
        URI integrationId, calculusId;
        calculusId = newTopic().name("calculus").getPublicId();
        var resource = newResource();
        resource.setName("Introduction to integration");
        integrationId = resource.getPublicId();

        URI id = getId(testUtils.createResource("/v1/node-resources", new NodeResources.AddResourceToNodeCommand() {
            {
                nodeId = calculusId;
                resourceId = integrationId;
            }
        }));

        final var connection = nodeResourceRepository.findByPublicId(id);
        assertTrue(connection.isPrimary().orElseThrow());

        final var calculus = nodeRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getResources().size());
        assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
        assertNotNull(nodeResourceRepository.getByPublicId(id));
        assertTrue(calculus.getNodeResources().iterator().next().isPrimary().orElseThrow());
    }

    @Test
    public void can_add_secondary_resource_to_node() throws Exception {
        final var calculusId = newTopic().name("calculus").getPublicId();
        var resource = newResource();
        resource.setName("Introduction to integration");
        final var integrationId = resource.getPublicId();

        final var node2 = newTopic();
        final var node2Id = node2.getPublicId();

        // 20190819 JEP@Cerpus: Behavior change: It was possible to create a non-primary resource
        // when no resource existed
        // before, but it has changed. The first connection to a resource will ignore the primary
        // parameter and will be
        // forced to become primary (subject-topics already did this)

        final var id = getId(
                testUtils.createResource("/v1/node-resources", new NodeResources.AddResourceToNodeCommand() {
                    {
                        nodeId = calculusId;
                        resourceId = integrationId;
                        primary = false;
                    }
                }));

        final var calculus = nodeRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getResources().size());
        assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
        assertNotNull(nodeResourceRepository.getByPublicId(id));
        // First topic connection will always be primary
        assertTrue(calculus.getNodeResources().iterator().next().isPrimary().orElseThrow());

        // After behavior change: Add the resource again to another topic with primary = false
        // should create a non-primary resource connection
        final var resource2ConnectionPublicId = getId(
                testUtils.createResource("/v1/node-resources", new NodeResources.AddResourceToNodeCommand() {
                    {
                        nodeId = node2Id;
                        resourceId = integrationId;
                        primary = false;
                    }
                }));

        final var resource2Connection = nodeResourceRepository.findFirstByPublicId(resource2ConnectionPublicId)
                .orElse(null);

        assertNotNull(resource2Connection);
        assertSame(node2, resource2Connection.getNode().orElse(null));
        assertSame(resource, resource2Connection.getResource().orElse(null));
        assertFalse(resource2Connection.isPrimary().orElseThrow());

        assertEquals(1, node2.getResources().size());
        assertEquals(2, resource.getNodeResources().size());
    }

    @Test
    public void cannot_add_existing_resource_to_node() throws Exception {
        final var calculus = newTopic().name("calculus");
        final var integration = newResource();
        integration.setName("Introduction to integration");
        NodeResource.create(calculus, integration);

        final var calculusId = calculus.getPublicId();
        final var integrationId = integration.getPublicId();

        testUtils.createResource("/v1/node-resources", new NodeResources.AddResourceToNodeCommand() {
            {
                nodeId = calculusId;
                resourceId = integrationId;
            }
        }, status().isConflict());
    }

    @Test
    public void can_delete_node_resource() throws Exception {
        URI id = save(NodeResource.create(newTopic(), newResource())).getPublicId();
        testUtils.deleteResource("/v1/node-resources/" + id);
        assertNull(nodeRepository.findByPublicId(id));
    }

    @Test
    public void can_update_node_resource() throws Exception {
        URI id = save(NodeResource.create(newTopic(), newResource())).getPublicId();

        testUtils.updateResource("/v1/node-resources/" + id, new NodeResources.UpdateNodeResourceCommand() {
            {
                primary = true;
            }
        });

        assertTrue(nodeResourceRepository.getByPublicId(id).isPrimary().orElseThrow());
    }

    @Test
    public void cannot_unset_primary_node() throws Exception {
        URI id = save(NodeResource.create(newTopic(), newResource(), true)).getPublicId();

        testUtils.updateResource("/v1/node-resources/" + id, new NodeResources.UpdateNodeResourceCommand() {
            {
                primary = false;
            }
        }, status().is4xxClientError());
    }

    @Test
    public void deleted_primary_node_is_replaced() throws Exception {
        Resource resource = builder.resource(r -> r.name("resource"));
        Node primary = builder.node(NodeType.TOPIC, t -> t.name("primary").resource(resource));
        builder.node(NodeType.TOPIC, t -> t.name("other").resource(resource, true));

        testUtils.deleteResource("/v1/nodes/" + primary.getPublicId());

        assertEquals("other", resource.getPrimaryNode().get().getName());
    }

    @Test
    public void can_get_resources() throws Exception {
        Node electricity = newTopic().name("electricity");
        Resource alternatingCurrent = newResource();
        alternatingCurrent.setName("How alternating current works");
        save(NodeResource.create(electricity, alternatingCurrent));

        Node calculus = newTopic().name("calculus");
        Resource integration = newResource();
        integration.setName("Introduction to integration");
        save(NodeResource.create(calculus, integration));

        MockHttpServletResponse response = testUtils.getResource("/v1/node-resources");
        NodeResources.NodeResourceDto[] topicResources = testUtils.getObject(NodeResources.NodeResourceDto[].class,
                response);

        assertEquals(2, topicResources.length);
        assertAnyTrue(topicResources, t -> electricity.getPublicId().equals(t.nodeId)
                && alternatingCurrent.getPublicId().equals(t.resourceId));
        assertAnyTrue(topicResources,
                t -> calculus.getPublicId().equals(t.nodeId) && integration.getPublicId().equals(t.resourceId));
        assertAllTrue(topicResources, t -> isValidId(t.id));
    }

    @Test
    public void can_get_resource_connections_paginated() throws Exception {
        List<NodeResource> connections = createTenContiguousRankedConnections();

        MockHttpServletResponse response = testUtils.getResource("/v1/node-resources/page?page=1&pageSize=5");
        NodeResources.NodeResourceDtoPage page1 = testUtils.getObject(NodeResources.NodeResourceDtoPage.class,
                response);
        assertEquals(5, page1.results.size());

        MockHttpServletResponse response2 = testUtils.getResource("/v1/node-resources/page?page=2&pageSize=5");
        NodeResources.NodeResourceDtoPage page2 = testUtils.getObject(NodeResources.NodeResourceDtoPage.class,
                response2);
        assertEquals(5, page2.results.size());

        var result = Stream.concat(page1.results.stream(), page2.results.stream()).collect(Collectors.toList());

        assertTrue(connections.stream().map(DomainEntity::getPublicId).collect(Collectors.toList())
                .containsAll(result.stream().map(r -> r.id).collect(Collectors.toList())));
    }

    @Test
    public void pagination_fails_if_param_not_present() throws Exception {
        MockHttpServletResponse response = testUtils.getResource("/v1/node-resources/page?page=0",
                status().isBadRequest());
        assertEquals(400, response.getStatus());

        MockHttpServletResponse response2 = testUtils.getResource("/v1/node-resources/page?pageSize=5",
                status().isBadRequest());
        assertEquals(400, response2.getStatus());
    }

    @Test
    public void can_get_topic_resource() throws Exception {
        Node electricity = newTopic().name("electricity");
        Resource alternatingCurrent = newResource();
        alternatingCurrent.setName("How alternating current works");
        NodeResource topicResource = save(NodeResource.create(electricity, alternatingCurrent));

        MockHttpServletResponse resource = testUtils.getResource("/v1/node-resources/" + topicResource.getPublicId());
        NodeResources.NodeResourceDto topicResourceIndexDocument = testUtils
                .getObject(NodeResources.NodeResourceDto.class, resource);
        assertEquals(electricity.getPublicId(), topicResourceIndexDocument.nodeId);
        assertEquals(alternatingCurrent.getPublicId(), topicResourceIndexDocument.resourceId);
    }

    @Test
    public void resource_can_only_have_one_primary_node() throws Exception {
        Resource graphs = builder.resource(r -> r.name("graphs"));

        builder.node(NodeType.TOPIC, t -> t.name("elementary maths").resource(graphs));

        Node graphTheory = builder.node(NodeType.TOPIC, t -> t.name("graph theory"));

        testUtils.createResource("/v1/node-resources", new NodeResources.AddResourceToNodeCommand() {
            {
                nodeId = graphTheory.getPublicId();
                resourceId = graphs.getPublicId();
                primary = true;
            }
        });

        graphs.getNodeResources().forEach(nodeResource -> {
            if (nodeResource.getNode().orElseThrow(RuntimeException::new).equals(graphTheory)) {
                assertTrue(nodeResource.isPrimary().orElseThrow());
            } else {
                assertFalse(nodeResource.isPrimary().orElseThrow());
            }
        });
    }

    @Test
    public void can_order_resources() throws Exception {
        Node geometry = builder.node(NodeType.TOPIC, t -> t.name("Geometry").publicId("urn:topic:1"));
        Resource squares = builder.resource(r -> r.name("Squares").publicId("urn:resource:1"));
        Resource circles = builder.resource(r -> r.name("Circles").publicId("urn:resource:2"));

        URI geometrySquares = save(NodeResource.create(geometry, squares)).getPublicId();
        URI geometryCircles = save(NodeResource.create(geometry, circles)).getPublicId();
        testUtils.updateResource("/v1/node-resources/" + geometryCircles,
                new NodeResources.UpdateNodeResourceCommand() {
                    {
                        primary = true;
                        id = geometryCircles;
                        rank = 1;
                    }
                });
        testUtils.updateResource("/v1/node-resources/" + geometrySquares,
                new NodeResources.UpdateNodeResourceCommand() {
                    {
                        primary = true;
                        id = geometrySquares;
                        rank = 2;
                    }
                });

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/" + geometry.getPublicId() + "/resources");
        ResourceWithNodeConnectionDTO[] resources = testUtils.getObject(ResourceWithNodeConnectionDTO[].class,
                response);
        assertEquals(circles.getPublicId(), resources[0].getId());
        assertEquals(squares.getPublicId(), resources[1].getId());
    }

    @Test
    public void resources_can_have_default_rank() throws Exception {
        builder.node(NodeType.TOPIC,
                t -> t.name("elementary maths").resource(r -> r.name("graphs")).resource(r -> r.name("sets")));

        MockHttpServletResponse response = testUtils.getResource("/v1/node-resources");
        NodeResources.NodeResourceDto[] topicResources = testUtils.getObject(NodeResources.NodeResourceDto[].class,
                response);
        assertAllTrue(topicResources, tr -> tr.rank == 0);
    }

    @Test
    public void can_create_resources_with_rank() throws Exception {
        Node geometry = builder.node(NodeType.TOPIC, t -> t.name("Geometry").publicId("urn:topic:1"));
        Resource squares = builder.resource(r -> r.name("Squares").publicId("urn:resource:1"));
        Resource circles = builder.resource(r -> r.name("Circles").publicId("urn:resource:2"));

        testUtils.createResource("/v1/node-resources", new NodeResources.AddResourceToNodeCommand() {
            {
                primary = true;
                nodeId = geometry.getPublicId();
                resourceId = squares.getPublicId();
                rank = 2;
            }
        });

        testUtils.createResource("/v1/node-resources", new NodeResources.AddResourceToNodeCommand() {
            {
                primary = true;
                nodeId = geometry.getPublicId();
                resourceId = circles.getPublicId();
                rank = 1;
            }
        });

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/" + geometry.getPublicId() + "/resources");
        final var resources = testUtils.getObject(ResourceWithNodeConnectionDTO[].class, response);

        assertEquals(circles.getPublicId(), resources[0].getId());
        assertEquals(squares.getPublicId(), resources[1].getId());
    }

    @Test
    public void update_child_resource_rank_modifies_other_contiguous_ranks() throws Exception {
        List<NodeResource> nodeResources = createTenContiguousRankedConnections(); // creates ranks 1, 2, 3, 4, 5, 6,
                                                                                   // 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeResources);

        // make the last object the first
        NodeResource updatedConnection = nodeResources.get(nodeResources.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-resources/" + updatedConnection.getPublicId().toString(),
                new NodeResources.UpdateNodeResourceCommand() {
                    {
                        primary = true;
                        rank = 1;
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (NodeResource nodeResource : nodeResources) {
            MockHttpServletResponse response = testUtils
                    .getResource("/v1/node-resources/" + nodeResource.getPublicId().toString());
            NodeResources.NodeResourceDto connectionFromDb = testUtils.getObject(NodeResources.NodeResourceDto.class,
                    response);
            // verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_child_resource_rank_does_not_alter_noncontiguous_ranks() throws Exception {

        List<NodeResource> nodeResources = createTenNonContiguousRankedConnections(); // creates ranks 1, 2, 3, 4, 5,
                                                                                      // 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeResources);

        // make the last object the first
        NodeResource updatedConnection = nodeResources.get(nodeResources.size() - 1);
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-resources/" + updatedConnection.getPublicId().toString(),
                new NodeResources.UpdateNodeResourceCommand() {
                    {
                        primary = true;
                        rank = 1;
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (NodeResource nodeResource : nodeResources) {
            MockHttpServletResponse response = testUtils
                    .getResource("/v1/node-resources/" + nodeResource.getPublicId().toString());
            NodeResources.NodeResourceDto connectionFromDb = testUtils.getObject(NodeResources.NodeResourceDto.class,
                    response);
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
    public void update_child_resource_rank_higher_rank_does_not_modify_existing_connections() throws Exception {
        List<NodeResource> nodeResources = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeResources);

        // set rank for last object to higher than any existing
        NodeResource updatedConnection = nodeResources.get(nodeResources.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-resources/" + nodeResources.get(9).getPublicId().toString(),
                new NodeResources.UpdateNodeResourceCommand() {
                    {
                        primary = true;
                        rank = 99;
                    }
                });
        assertEquals(99, updatedConnection.getRank());

        // verify that the other connections are unchanged
        for (NodeResource nodeResource : nodeResources) {
            MockHttpServletResponse response = testUtils
                    .getResource("/v1/node-resources/" + nodeResource.getPublicId().toString());
            NodeResources.NodeResourceDto connection = testUtils.getObject(NodeResources.NodeResourceDto.class,
                    response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    @Test
    public void update_metadata_for_connection() throws Exception {
        URI id = save(NodeResource.create(newTopic(), newResource())).getPublicId();
        testUtils.updateResource("/v1/node-resources/" + id + "/metadata", new MetadataDto() {
            {
                visible = false;
                grepCodes = Set.of("KM123");
                customFields = Map.of("key", "value");
            }
        }, status().isOk());
        NodeResource connection = nodeResourceRepository.getByPublicId(id);
        assertFalse(connection.getMetadata().isVisible());
        Set<String> codes = connection.getMetadata().getGrepCodes().stream().map(GrepCode::getCode)
                .collect(Collectors.toSet());
        assertTrue(codes.contains("KM123"));
        Collection<CustomFieldValue> customFieldValues = connection.getMetadata().getCustomFieldValues();
        assertTrue(customFieldValues.stream().map(CustomFieldValue::getCustomField).map(CustomField::getKey)
                .collect(Collectors.toSet()).contains("key"));
        assertTrue(customFieldValues.stream().map(CustomFieldValue::getValue).collect(Collectors.toSet())
                .contains("value"));

    }

    private Map<String, Integer> mapConnectionRanks(List<NodeResource> nodeResources) {
        Map<String, Integer> mappedRanks = new HashMap<>();
        for (NodeResource nr : nodeResources) {
            mappedRanks.put(nr.getPublicId().toString(), nr.getRank());
        }
        return mappedRanks;
    }

    private List<NodeResource> createTenContiguousRankedConnections() {
        List<NodeResource> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Resource sub = newResource();
            NodeResource nodeResource = NodeResource.create(parent, sub);
            nodeResource.setRank(i);
            connections.add(nodeResource);
            save(nodeResource);
        }
        return connections;
    }

    private List<NodeResource> createTenNonContiguousRankedConnections() {
        List<NodeResource> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Resource sub = newResource();
            NodeResource nodeResource = NodeResource.create(parent, sub);
            if (i <= 5) {
                nodeResource.setRank(i);
            } else {
                nodeResource.setRank(i * 10);
            }
            connections.add(nodeResource);
            save(nodeResource);
        }
        return connections;
    }
}
