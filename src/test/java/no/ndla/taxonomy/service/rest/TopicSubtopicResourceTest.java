package no.ndla.taxonomy.service.rest;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;
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
    private TitanGraph graph;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_add_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        try (TitanTransaction transaction = graph.newTransaction()) {
            calculusId = new Topic(transaction).name("calculus").getId();
            integrationId = new Topic(transaction).name("integration").getId();
        }

        String id = getId(
                createResource("/topic-subtopics", new TopicSubtopicResource.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }})
        );

        try (TitanTransaction transaction = graph.newTransaction()) {
            Topic calculus = Topic.getById(calculusId.toString(), transaction);
            assertEquals(1, count(calculus.getSubtopics()));
            assertAnyTrue(calculus.getSubtopics(), t -> "integration".equals(t.getName()));
            assertNotNull(TopicSubtopic.getById(id, transaction));
        }
    }

    @Test
    public void cannot_add_existing_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        try (TitanTransaction transaction = graph.newTransaction()) {
            Topic calculus = new Topic(transaction).name("calculus");
            Topic integration = new Topic(transaction).name("integration");
            calculus.addSubtopic(integration);

            calculusId = calculus.getId();
            integrationId = integration.getId();
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
        try (TitanTransaction transaction = graph.newTransaction()) {
            id = new Topic(transaction).addSubtopic(new Topic(transaction)).getId().toString();
        }

        deleteResource("/topic-subtopics/" + id);
        assertNotFound(transaction -> Topic.getById(id, transaction));
    }

    @Test
    public void can_update_topic_subtopic() throws Exception {
        String id;
        try (TitanTransaction transaction = graph.newTransaction()) {
            id = new Topic(transaction).addSubtopic(new Topic(transaction)).getId().toString();
        }

        TopicSubtopicResource.UpdateTopicSubtopicCommand command = new TopicSubtopicResource.UpdateTopicSubtopicCommand();
        command.primary = true;

        updateResource("/topic-subtopics/" + id, command);

        try (TitanTransaction transaction = graph.newTransaction()) {
            assertTrue(TopicSubtopic.getById(id, transaction).isPrimary());
        }
    }

    @Test
    public void can_get_topics() throws Exception {
        URI alternatingCurrentId, electricityId, calculusId, integrationId;

        try (TitanTransaction transaction = graph.newTransaction()) {
            Topic electricity = new Topic(transaction).name("electricity");
            Topic alternatingCurrent = new Topic(transaction).name("alternating current");
            electricity.addSubtopic(alternatingCurrent);

            Topic calculus = new Topic(transaction).name("calculus");
            Topic integration = new Topic(transaction).name("integration");
            calculus.addSubtopic(integration);

            electricityId = electricity.getId();
            alternatingCurrentId = alternatingCurrent.getId();
            calculusId = calculus.getId();
            integrationId = integration.getId();
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
        try (TitanTransaction transaction = graph.newTransaction()) {
            Topic electricity = new Topic(transaction).name("electricity");
            Topic alternatingCurrent = new Topic(transaction).name("alternating current");
            TopicSubtopic topicSubtopic = electricity.addSubtopic(alternatingCurrent);

            topicid = electricity.getId();
            subtopicid = alternatingCurrent.getId();
            id = topicSubtopic.getId();
        }

        MockHttpServletResponse resource = getResource("/topic-subtopics/" + id);
        TopicSubtopicResource.TopicSubtopicIndexDocument topicSubtopicIndexDocument = getObject(TopicSubtopicResource.TopicSubtopicIndexDocument.class, resource);
        assertEquals(topicid, topicSubtopicIndexDocument.topicid);
        assertEquals(subtopicid, topicSubtopicIndexDocument.subtopicid);
    }
}
