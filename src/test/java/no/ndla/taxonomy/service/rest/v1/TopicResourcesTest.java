package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicResource;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicResourcesTest extends RestTest {

    @Test
    public void can_add_resource_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = newTopic().name("calculus").getPublicId();
        integrationId = newResource().name("Introduction to integration").getPublicId();

        URI id = getId(
                createResource("/v1/topic-resources", new TopicResources.AddResourceToTopicCommand() {{
                    topicid = calculusId;
                    resourceid = integrationId;
                }})
        );

        Topic calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, count(calculus.getResources()));
        assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
        assertNotNull(topicResourceRepository.getByPublicId(id));
    }

    @Test
    public void cannot_add_existing_resource_to_topic() throws Exception {
        URI integrationId, calculusId;
        Topic calculus = newTopic().name("calculus");
        Resource integration = newResource().name("Introduction to integration");
        calculus.addResource(integration);

        calculusId = calculus.getPublicId();
        integrationId = integration.getPublicId();

        createResource("/v1/topic-resources",
                new TopicResources.AddResourceToTopicCommand() {
                    {
                        topicid = calculusId;
                        resourceid = integrationId;
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
        Resource alternatingCurrent = newResource().name("How alternating current works");
        save(electricity.addResource(alternatingCurrent));

        Topic calculus = newTopic().name("calculus");
        Resource integration = newResource().name("Introduction to integration");
        save(calculus.addResource(integration));

        MockHttpServletResponse response = getResource("/v1/topic-resources");
        TopicResources.TopicResourceIndexDocument[] topicResources = getObject(TopicResources.TopicResourceIndexDocument[].class, response);

        assertEquals(2, topicResources.length);
        assertAnyTrue(topicResources, t -> electricity.getPublicId().equals(t.topicid) && alternatingCurrent.getPublicId().equals(t.resourceid));
        assertAnyTrue(topicResources, t -> calculus.getPublicId().equals(t.topicid) && integration.getPublicId().equals(t.resourceid));
        assertAllTrue(topicResources, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topic_resource() throws Exception {
        Topic electricity = newTopic().name("electricity");
        Resource alternatingCurrent = newResource().name("How alternating current works");
        TopicResource topicResource = save(electricity.addResource(alternatingCurrent));

        MockHttpServletResponse resource = getResource("/v1/topic-resources/" + topicResource.getPublicId());
        TopicResources.TopicResourceIndexDocument topicResourceIndexDocument = getObject(TopicResources.TopicResourceIndexDocument.class, resource);
        assertEquals(electricity.getPublicId(), topicResourceIndexDocument.topicid);
        assertEquals(alternatingCurrent.getPublicId(), topicResourceIndexDocument.resourceid);
    }

    @Test
    public void first_topic_connected_to_resource_is_primary() throws Exception {
        Topic electricity = newTopic().name("electricity");
        Resource alternatingCurrent = newResource().name("How alternating current works");
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
            resourceid = graphs.getPublicId();
            primary = true;
        }});

        graphs.topics.forEach(topicResource -> {
            if (topicResource.getTopic().equals(graphTheory)) assertTrue(topicResource.isPrimary());
            else assertFalse(topicResource.isPrimary());
        });
    }
}
