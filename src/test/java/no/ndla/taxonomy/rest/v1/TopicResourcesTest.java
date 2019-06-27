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
                createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
                    topicid = calculusId;
                    resourceId = integrationId;
                }})
        );

        Topic calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, count(calculus.getResources()));
        assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
        assertNotNull(topicResourceRepository.getByPublicId(id));
        assertTrue(calculus.getTopicResources().iterator().next().isPrimary());
    }

    @Test
    public void can_add_secondary_resource_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = newTopic().name("calculus").getPublicId();
        var resource = newResource();
        resource.setName("Introduction to integration");
        integrationId = resource.getPublicId();

        URI id = getId(
                createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
                    topicid = calculusId;
                    resourceId = integrationId;
                    primary = false;
                }})
        );

        Topic calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, count(calculus.getResources()));
        assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
        assertNotNull(topicResourceRepository.getByPublicId(id));
        assertFalse(calculus.getTopicResources().iterator().next().isPrimary());
    }

    @Test
    public void cannot_add_existing_resource_to_topic() throws Exception {
        URI integrationId, calculusId;
        Topic calculus = newTopic().name("calculus");
        var integration = newResource();
        integration.setName("Introduction to integration");

        calculusId = calculus.getPublicId();
        integrationId = integration.getPublicId();

        createResource("/v1/topic-resources",
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
        URI id = save(newTopic().addResource(newResource())).getPublicId();
        deleteResource("/v1/topic-resources/" + id);
        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_update_topic_resource() throws Exception {
        URI id = save(newTopic().addResource(newResource())).getPublicId();

        updateResource("/v1/topic-resources/" + id, new TopicResources.UpdateTopicResourceCommand() {{
            primary = true;
        }});

        assertTrue(topicResourceRepository.getByPublicId(id).isPrimary());
    }

    @Test
    public void cannot_unset_primary_topic() throws Exception {
        URI id = save(newTopic().addResource(newResource())).getPublicId();

        updateResource("/v1/topic-resources/" + id, new TopicResources.UpdateTopicResourceCommand() {{
            primary = false;
        }}, status().is4xxClientError());
    }

    @Test
    public void deleted_primary_topic_is_replaced() throws Exception {
        Resource resource = builder.resource(r -> r.name("resource"));
        Topic primary = builder.topic(t -> t.name("primary").resource(resource));
        Topic other = builder.topic(t -> t.name("other").resource(resource));
        resource.setPrimaryTopic(primary);

        deleteResource("/v1/topics/" + primary.getPublicId());

        assertEquals("other", resource.getPrimaryTopic().getName());
    }

    @Test
    public void can_get_resources() throws Exception {
        Topic electricity = newTopic().name("electricity");
        Resource alternatingCurrent = newResource();
        alternatingCurrent.setName("How alternating current works");
        save(electricity.addResource(alternatingCurrent));

        Topic calculus = newTopic().name("calculus");
        Resource integration = newResource();
        integration.setName("Introduction to integration");
        save(calculus.addResource(integration));

        MockHttpServletResponse response = getResource("/v1/topic-resources");
        TopicResources.TopicResourceIndexDocument[] topicResources = getObject(TopicResources.TopicResourceIndexDocument[].class, response);

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
        TopicResource topicResource = save(electricity.addResource(alternatingCurrent));

        MockHttpServletResponse resource = getResource("/v1/topic-resources/" + topicResource.getPublicId());
        TopicResources.TopicResourceIndexDocument topicResourceIndexDocument = getObject(TopicResources.TopicResourceIndexDocument.class, resource);
        assertEquals(electricity.getPublicId(), topicResourceIndexDocument.topicid);
        assertEquals(alternatingCurrent.getPublicId(), topicResourceIndexDocument.resourceId);
    }

    @Test
    public void first_topic_connected_to_resource_is_primary() throws Exception {
        Topic electricity = newTopic().name("electricity");
        Resource alternatingCurrent = newResource();
        alternatingCurrent.setName("How alternating current works");
        TopicResource topicResource = save(electricity.addResource(alternatingCurrent));

        MockHttpServletResponse resource = getResource("/v1/topic-resources/" + topicResource.getPublicId());
        TopicResources.TopicResourceIndexDocument topicResourceIndexDocument = getObject(TopicResources.TopicResourceIndexDocument.class, resource);
        assertTrue(topicResourceIndexDocument.primary);
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

        createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
            topicid = graphTheory.getPublicId();
            resourceId = graphs.getPublicId();
            primary = true;
        }});

        graphs.getTopicResources().forEach(topicResource -> {
            if (topicResource.getTopic().equals(graphTheory)) assertTrue(topicResource.isPrimary());
            else assertFalse(topicResource.isPrimary());
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


        URI geometrySquares = save(geometry.addResource(squares)).getPublicId();
        URI geometryCircles = save(geometry.addResource(circles)).getPublicId();
        updateResource("/v1/topic-resources/" + geometryCircles, new TopicResources.UpdateTopicResourceCommand() {{
            primary = true;
            id = geometryCircles;
            rank = 1;
        }});
        updateResource("/v1/topic-resources/" + geometrySquares, new TopicResources.UpdateTopicResourceCommand() {{
            primary = true;
            id = geometrySquares;
            rank = 2;
        }});

        MockHttpServletResponse response = getResource("/v1/topics/" + geometry.getPublicId() + "/resources");
        ResourceIndexDocument[] resources = getObject(ResourceIndexDocument[].class, response);
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

        MockHttpServletResponse response = getResource("/v1/topic-resources");
        TopicResources.TopicResourceIndexDocument[] topicResources = getObject(TopicResources.TopicResourceIndexDocument[].class, response);
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

        createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
            primary = true;
            topicid = geometry.getPublicId();
            resourceId = squares.getPublicId();
            rank = 2;
        }});

        createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
            primary = true;
            topicid = geometry.getPublicId();
            resourceId = circles.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = getResource("/v1/topics/" + geometry.getPublicId() + "/resources");
        TopicResources.TopicResourceIndexDocument[] resources = getObject(TopicResources.TopicResourceIndexDocument[].class, response);

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
        updateResource("/v1/topic-resources/" + updatedConnection.getPublicId().toString(), new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicResource topicResource : topicResources) {
            MockHttpServletResponse response = getResource("/v1/topic-resources/" + topicResource.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connectionFromDb = getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
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
        updateResource("/v1/topic-resources/" + updatedConnection.getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicResource topicResource : topicResources) {
            MockHttpServletResponse response = getResource("/v1/topic-resources/" + topicResource.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connectionFromDb = getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
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
        updateResource("/v1/topic-resources/" + topicResources.get(9).getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 99;
        }});
        assertEquals(99, updatedConnection.getRank());

        //verify that the other connections are unchanged
        for (TopicResource topicResource : topicResources) {
            MockHttpServletResponse response = getResource("/v1/topic-resources/" + topicResource.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connection = getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
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
            TopicResource topicResource = parent.addResource(sub);
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
            TopicResource topicSubtopic = parent.addResource(sub);
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
