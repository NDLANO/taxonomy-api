package no.ndla.taxonomy.service.rest;


import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicResource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
public class TopicResourceControllerTest {

    @Autowired
    private GraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_add_resource_to_topic() throws Exception {
        URI integrationId, calculusId;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            calculusId = new Topic(graph).name("calculus").getId();
            integrationId = new Resource(graph).name("Introduction to integration").getId();
            transaction.commit();
        }

        String id = getId(
                createResource("/topic-resources", new TopicResourceController.AddResourceToTopicCommand() {{
                    topicid = calculusId;
                    resourceid = integrationId;
                }})
        );

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Topic calculus = Topic.getById(calculusId.toString(), graph);
            assertEquals(1, count(calculus.getResources()));
            assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
            assertNotNull(TopicResource.getById(id, graph));
            transaction.rollback();
        }
    }

    @Test
    public void cannot_add_existing_resource_to_topic() throws Exception {
        URI integrationId, calculusId;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Topic calculus = new Topic(graph).name("calculus");
            Resource integration = new Resource(graph).name("Introduction to integration");
            calculus.addResource(integration);

            calculusId = calculus.getId();
            integrationId = integration.getId();
            transaction.commit();
        }

        createResource("/topic-resources",
                new TopicResourceController.AddResourceToTopicCommand() {{
                    topicid = calculusId;
                    resourceid = integrationId;
                }},
                status().isConflict()
        );
    }


    @Test
    public void can_delete_resource_topic() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Topic(graph).addResource(new Resource(graph)).getId().toString();
            transaction.commit();
        }

        deleteResource("/topic-resources/" + id);
        assertNotFound(graph -> Topic.getById(id, graph));
    }

    @Test
    public void can_update_topic_resource() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Topic(graph).addResource(new Resource(graph)).getId().toString();
            transaction.commit();
        }

        TopicResourceController.UpdateTopicResourceCommand command = new TopicResourceController.UpdateTopicResourceCommand();
        command.primary = true;

        updateResource("/topic-resources/" + id, command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            assertTrue(TopicResource.getById(id, graph).isPrimary());
            transaction.rollback();
        }
    }

    @Test
    public void can_get_resources() throws Exception {
        URI alternatingCurrentId, electricityId, calculusId, integrationId;

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Topic electricity = new Topic(graph).name("electricity");
            Resource alternatingCurrent = new Resource(graph).name("How alternating current works");
            electricity.addResource(alternatingCurrent);

            Topic calculus = new Topic(graph).name("calculus");
            Resource integration = new Resource(graph).name("Introduction to integration");
            calculus.addResource(integration);

            electricityId = electricity.getId();
            alternatingCurrentId = alternatingCurrent.getId();
            calculusId = calculus.getId();
            integrationId = integration.getId();
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/topic-resources");
        TopicResourceController.TopicResourceIndexDocument[] topicResources = getObject(TopicResourceController.TopicResourceIndexDocument[].class, response);

        assertEquals(2, topicResources.length);
        assertAnyTrue(topicResources, t -> electricityId.equals(t.topicid) && alternatingCurrentId.equals(t.resourceid));
        assertAnyTrue(topicResources, t -> calculusId.equals(t.topicid) && integrationId.equals(t.resourceid));
        assertAllTrue(topicResources, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topic_resource() throws Exception {
        URI topicid, resourceid, id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Topic electricity = new Topic(graph).name("electricity");
            Resource alternatingCurrent = new Resource(graph).name("How alternating current works");
            TopicResource topicResource = electricity.addResource(alternatingCurrent);

            topicid = electricity.getId();
            resourceid = alternatingCurrent.getId();
            id = topicResource.getId();
            transaction.commit();
        }

        MockHttpServletResponse resource = getResource("/topic-resources/" + id);
        TopicResourceController.TopicResourceIndexDocument topicResourceIndexDocument = getObject(TopicResourceController.TopicResourceIndexDocument.class, resource);
        assertEquals(topicid, topicResourceIndexDocument.topicid);
        assertEquals(resourceid, topicResourceIndexDocument.resourceid);
    }
}
