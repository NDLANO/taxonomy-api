package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicResourcesTest extends RestTest {

    @Test
    public void can_add_resource_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = newTopic().name("calculus").getPublicId();
        var resource = newResource();
        resource.setName("Introduction to integration");
        integrationId = resource.getPublicId();

        URI id = getId(
                testUtils.createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
                    topicid = calculusId;
                    resourceId = integrationId;
                }})
        );

        final var connection = topicResourceRepository.findByPublicId(id);
        assertTrue(connection.isPrimary());

        Topic calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getResources().size());
        assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
        assertNotNull(topicResourceRepository.getByPublicId(id));
        assertTrue(calculus.getTopicResources().iterator().next().isPrimary());
    }

    @Test
    public void can_add_secondary_resource_to_topic() throws Exception {
        final var calculusId = newTopic().name("calculus").getPublicId();
        var resource = newResource();
        resource.setName("Introduction to integration");
        final var integrationId = resource.getPublicId();

        final var topic2 = newTopic();
        final var topic2Id = topic2.getPublicId();

        // 20190819 JEP@Cerpus: Behavior change: It was possible to create a non-primary resource when no resource existed
        // before, but it has changed. The first connection to a resource will ignore the primary parameter and will be
        // forced to become primary (subject-topics already did this)

        final var id = getId(
                testUtils.createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
                    topicid = calculusId;
                    resourceId = integrationId;
                    primary = false;
                }})
        );

        final var calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getResources().size());
        assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
        assertNotNull(topicResourceRepository.getByPublicId(id));
        // First topic connection will always be primary
        assertTrue(calculus.getTopicResources().iterator().next().isPrimary());

        // After behavior change: Add the resource again to another topic with primary = false should create a non-primary resource connection
        final var resource2ConnectionPublicId = getId(
                testUtils.createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
                    topicid = topic2Id;
                    resourceId = integrationId;
                    primary = false;
                }})
        );

        final var resource2Connection = topicResourceRepository.findFirstByPublicId(resource2ConnectionPublicId).orElse(null);

        assertNotNull(resource2Connection);
        assertSame(topic2, resource2Connection.getTopic().orElse(null));
        assertSame(resource, resource2Connection.getResource().orElse(null));
        assertFalse(resource2Connection.isPrimary());

        assertEquals(1, topic2.getResources().size());
        assertEquals(2, resource.getTopicResources().size());
    }

    @Test
    public void cannot_add_existing_resource_to_topic() throws Exception {
        final var calculus = newTopic().name("calculus");
        final var integration = newResource();
        integration.setName("Introduction to integration");
        TopicResource.create(calculus, integration);

        final var calculusId = calculus.getPublicId();
        final var integrationId = integration.getPublicId();

        testUtils.createResource("/v1/topic-resources",
                new TopicResources.AddResourceToTopicCommand() {
                    {
                        topicid = calculusId;
                        resourceId = integrationId;
                    }
                },
                status().isConflict()
        );
    }


    @Test
    public void can_delete_topic_resource() throws Exception {
        URI id = save(TopicResource.create(newTopic(), newResource())).getPublicId();
        testUtils.deleteResource("/v1/topic-resources/" + id);
        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_update_topic_resource() throws Exception {
        URI id = save(TopicResource.create(newTopic(), newResource())).getPublicId();

        testUtils.updateResource("/v1/topic-resources/" + id, new TopicResources.UpdateTopicResourceCommand() {{
            primary = true;
        }});

        assertTrue(topicResourceRepository.getByPublicId(id).isPrimary());
    }

    @Test
    public void cannot_unset_primary_topic() throws Exception {
        URI id = save(TopicResource.create(newTopic(), newResource(), true)).getPublicId();

        testUtils.updateResource("/v1/topic-resources/" + id, new TopicResources.UpdateTopicResourceCommand() {{
            primary = false;
        }}, status().is4xxClientError());
    }

    @Test
    public void deleted_primary_topic_is_replaced() throws Exception {
        Resource resource = builder.resource(r -> r.name("resource"));
        Topic primary = builder.topic(t -> t.name("primary").resource(resource));
        builder.topic(t -> t.name("other").resource(resource, true));

        testUtils.deleteResource("/v1/topics/" + primary.getPublicId());

        assertEquals("other", resource.getPrimaryTopic().get().getName());
    }

    @Test
    public void can_get_resources() throws Exception {
        Topic electricity = newTopic().name("electricity");
        Resource alternatingCurrent = newResource();
        alternatingCurrent.setName("How alternating current works");
        save(TopicResource.create(electricity, alternatingCurrent));

        Topic calculus = newTopic().name("calculus");
        Resource integration = newResource();
        integration.setName("Introduction to integration");
        save(TopicResource.create(calculus, integration));

        MockHttpServletResponse response = testUtils.getResource("/v1/topic-resources");
        TopicResources.TopicResourceIndexDocument[] topicResources = testUtils.getObject(TopicResources.TopicResourceIndexDocument[].class, response);

        assertEquals(2, topicResources.length);
        assertAnyTrue(topicResources, t -> electricity.getPublicId().equals(t.topicid) && alternatingCurrent.getPublicId().equals(t.resourceId));
        assertAnyTrue(topicResources, t -> calculus.getPublicId().equals(t.topicid) && integration.getPublicId().equals(t.resourceId));
        assertAllTrue(topicResources, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topic_resource() throws Exception {
        Topic electricity = newTopic().name("electricity");
        Resource alternatingCurrent = newResource();
        alternatingCurrent.setName("How alternating current works");
        TopicResource topicResource = save(TopicResource.create(electricity, alternatingCurrent));

        MockHttpServletResponse resource = testUtils.getResource("/v1/topic-resources/" + topicResource.getPublicId());
        TopicResources.TopicResourceIndexDocument topicResourceIndexDocument = testUtils.getObject(TopicResources.TopicResourceIndexDocument.class, resource);
        assertEquals(electricity.getPublicId(), topicResourceIndexDocument.topicid);
        assertEquals(alternatingCurrent.getPublicId(), topicResourceIndexDocument.resourceId);
    }

    @Test
    public void resource_can_only_have_one_primary_topic() throws Exception {
        Resource graphs = builder.resource(r -> r.name("graphs"));

        builder.topic(t -> t
                .name("elementary maths")
                .resource(graphs)
        );

        Topic graphTheory = builder.topic(t -> t
                .name("graph theory"));

        testUtils.createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
            topicid = graphTheory.getPublicId();
            resourceId = graphs.getPublicId();
            primary = true;
        }});

        graphs.getTopicResources().forEach(topicResource -> {
            if (topicResource.getTopic().orElseThrow(RuntimeException::new).equals(graphTheory)) {
                assertTrue(topicResource.isPrimary());
            } else {
                assertFalse(topicResource.isPrimary());
            }
        });
    }

    @Test
    public void can_order_resources() throws Exception {
        Topic geometry = builder.topic(t -> t
                .name("Geometry")
                .publicId("urn:topic:1"));
        Resource squares = builder.resource(r -> r
                .name("Squares")
                .publicId("urn:resource:1"));
        Resource circles = builder.resource(r -> r
                .name("Circles")
                .publicId("urn:resource:2"));


        URI geometrySquares = save(TopicResource.create(geometry, squares)).getPublicId();
        URI geometryCircles = save(TopicResource.create(geometry, circles)).getPublicId();
        testUtils.updateResource("/v1/topic-resources/" + geometryCircles, new TopicResources.UpdateTopicResourceCommand() {{
            primary = true;
            id = geometryCircles;
            rank = 1;
        }});
        testUtils.updateResource("/v1/topic-resources/" + geometrySquares, new TopicResources.UpdateTopicResourceCommand() {{
            primary = true;
            id = geometrySquares;
            rank = 2;
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/" + geometry.getPublicId() + "/resources");
        ResourceIndexDocument[] resources = testUtils.getObject(ResourceIndexDocument[].class, response);
        assertEquals(circles.getPublicId(), resources[0].id);
        assertEquals(squares.getPublicId(), resources[1].id);
    }

    @Test
    public void resources_can_have_default_rank() throws Exception {
        builder.topic(t -> t
                .name("elementary maths")
                .resource(r -> r.name("graphs"))
                .resource(r -> r.name("sets"))
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topic-resources");
        TopicResources.TopicResourceIndexDocument[] topicResources = testUtils.getObject(TopicResources.TopicResourceIndexDocument[].class, response);
        assertAllTrue(topicResources, tr -> tr.rank == 0);
    }

    @Test
    public void can_create_resources_with_rank() throws Exception {
        Topic geometry = builder.topic(t -> t
                .name("Geometry")
                .publicId("urn:topic:1"));
        Resource squares = builder.resource(r -> r
                .name("Squares")
                .publicId("urn:resource:1"));
        Resource circles = builder.resource(r -> r
                .name("Circles")
                .publicId("urn:resource:2"));

        testUtils.createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
            primary = true;
            topicid = geometry.getPublicId();
            resourceId = squares.getPublicId();
            rank = 2;
        }});

        testUtils.createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
            primary = true;
            topicid = geometry.getPublicId();
            resourceId = circles.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/" + geometry.getPublicId() + "/resources");
        TopicResources.TopicResourceIndexDocument[] resources = testUtils.getObject(TopicResources.TopicResourceIndexDocument[].class, response);

        assertEquals(circles.getPublicId(), resources[0].id);
        assertEquals(squares.getPublicId(), resources[1].id);
    }

    @Test
    public void update_child_resource_rank_modifies_other_contiguous_ranks() throws Exception {
        List<TopicResource> topicResources = createTenContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicResources);

        //make the last object the first
        TopicResource updatedConnection = topicResources.get(topicResources.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/topic-resources/" + updatedConnection.getPublicId().toString(), new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicResource topicResource : topicResources) {
            MockHttpServletResponse response = testUtils.getResource("/v1/topic-resources/" + topicResource.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connectionFromDb = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
            //verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_child_resource_rank_does_not_alter_noncontiguous_ranks() throws Exception {

        List<TopicResource> topicResources = createTenNonContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicResources);

        //make the last object the first
        TopicResource updatedConnection = topicResources.get(topicResources.size() - 1);
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource("/v1/topic-resources/" + updatedConnection.getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicResource topicResource : topicResources) {
            MockHttpServletResponse response = testUtils.getResource("/v1/topic-resources/" + topicResource.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connectionFromDb = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
            //verify that only the contiguous connections are updated
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
        List<TopicResource> topicResources = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicResources);

        //set rank for last object to higher than any existing
        TopicResource updatedConnection = topicResources.get(topicResources.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/topic-resources/" + topicResources.get(9).getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 99;
        }});
        assertEquals(99, updatedConnection.getRank());

        //verify that the other connections are unchanged
        for (TopicResource topicResource : topicResources) {
            MockHttpServletResponse response = testUtils.getResource("/v1/topic-resources/" + topicResource.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connection = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    private Map<String, Integer> mapConnectionRanks(List<TopicResource> topicResources) {
        Map<String, Integer> mappedRanks = new HashMap<>();
        for (TopicResource tr : topicResources) {
            mappedRanks.put(tr.getPublicId().toString(), tr.getRank());
        }
        return mappedRanks;
    }


    private List<TopicResource> createTenContiguousRankedConnections() {
        List<TopicResource> connections = new ArrayList<>();
        Topic parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Resource sub = newResource();
            TopicResource topicResource = TopicResource.create(parent, sub);
            topicResource.setRank(i);
            connections.add(topicResource);
            save(topicResource);
        }
        return connections;
    }

    private List<TopicResource> createTenNonContiguousRankedConnections() {
        List<TopicResource> connections = new ArrayList<>();
        Topic parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Resource sub = newResource();
            TopicResource topicSubtopic = TopicResource.create(parent, sub);
            if (i <= 5) {
                topicSubtopic.setRank(i);
            } else {
                topicSubtopic.setRank(i * 10);
            }
            connections.add(topicSubtopic);
            save(topicSubtopic);
        }
        return connections;
    }
}
