package no.ndla.taxonomy.service.rest;


import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
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
public class TopicSubtopicResourceTest {

    @Autowired
    private OrientGraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_add_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            calculusId = new Topic(graph).name("calculus").getId();
            integrationId = new Topic(graph).name("integration").getId();
            transaction.commit();
        }

        String id = getId(
                createResource("/topic-subtopics", new TopicSubtopicResource.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }})
        );

        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic calculus = Topic.getById(calculusId.toString(), graph);
            assertEquals(1, count(calculus.getSubtopics()));
            assertAnyTrue(calculus.getSubtopics(), t -> "integration".equals(t.getName()));
            assertNotNull(TopicSubtopic.getById(id, graph));
            transaction.rollback();
        }
    }

    @Test
    public void cannot_add_existing_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic calculus = new Topic(graph).name("calculus");
            Topic integration = new Topic(graph).name("integration");
            calculus.addSubtopic(integration);

            calculusId = calculus.getId();
            integrationId = integration.getId();
            transaction.commit();
        }

        createResource("/topic-subtopics",
                new TopicSubtopicResource.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }},
                status().isConflict()
        );
    }


    @Test
    public void can_delete_subtopic_topic() throws Exception {
        String id;
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            id = new Topic(graph).addSubtopic(new Topic(graph)).getId().toString();
            transaction.commit();
        }

        deleteResource("/topic-subtopics/" + id);
        assertNotFound(graph -> Topic.getById(id, graph));
    }

    @Test
    public void can_update_topic_subtopic() throws Exception {
        String id;
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            id = new Topic(graph).addSubtopic(new Topic(graph)).getId().toString();
            transaction.commit();
        }

        TopicSubtopicResource.UpdateTopicSubtopicCommand command = new TopicSubtopicResource.UpdateTopicSubtopicCommand();
        command.primary = true;

        updateResource("/topic-subtopics/" + id, command);

        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            assertTrue(TopicSubtopic.getById(id, graph).isPrimary());
            transaction.rollback();
        }
    }

    @Test
    public void can_get_topics() throws Exception {
        URI alternatingCurrentId, electricityId, calculusId, integrationId;

        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic electricity = new Topic(graph).name("electricity");
            Topic alternatingCurrent = new Topic(graph).name("alternating current");
            electricity.addSubtopic(alternatingCurrent);

            Topic calculus = new Topic(graph).name("calculus");
            Topic integration = new Topic(graph).name("integration");
            calculus.addSubtopic(integration);

            electricityId = electricity.getId();
            alternatingCurrentId = alternatingCurrent.getId();
            calculusId = calculus.getId();
            integrationId = integration.getId();
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/topic-subtopics");
        TopicSubtopicResource.TopicSubtopicIndexDocument[] topicSubtopics = getObject(TopicSubtopicResource.TopicSubtopicIndexDocument[].class, response);

        assertEquals(2, topicSubtopics.length);
        assertAnyTrue(topicSubtopics, t -> electricityId.equals(t.topicid) && alternatingCurrentId.equals(t.subtopicid));
        assertAnyTrue(topicSubtopics, t -> calculusId.equals(t.topicid) && integrationId.equals(t.subtopicid));
        assertAllTrue(topicSubtopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topic_subtopic() throws Exception {
        URI topicid, subtopicid, id;
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic electricity = new Topic(graph).name("electricity");
            Topic alternatingCurrent = new Topic(graph).name("alternating current");
            TopicSubtopic topicSubtopic = electricity.addSubtopic(alternatingCurrent);

            topicid = electricity.getId();
            subtopicid = alternatingCurrent.getId();
            id = topicSubtopic.getId();
            transaction.commit();
        }

        MockHttpServletResponse resource = getResource("/topic-subtopics/" + id);
        TopicSubtopicResource.TopicSubtopicIndexDocument topicSubtopicIndexDocument = getObject(TopicSubtopicResource.TopicSubtopicIndexDocument.class, resource);
        assertEquals(topicid, topicSubtopicIndexDocument.topicid);
        assertEquals(subtopicid, topicSubtopicIndexDocument.subtopicid);
    }
}
