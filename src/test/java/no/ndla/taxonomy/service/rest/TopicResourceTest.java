package no.ndla.taxonomy.service.rest;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.Topic;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
public class TopicResourceTest {

    @Autowired
    private TitanGraph graph;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_get_all_topics() throws Exception {
        try (TitanTransaction transaction = graph.newTransaction()) {
            new Topic(transaction).name("photo synthesis");
            new Topic(transaction).name("trigonometry");
        }

        MockHttpServletResponse response = getResource("/topics");
        TopicResource.TopicIndexDocument[] topics = getObject(TopicResource.TopicIndexDocument[].class, response);
        assertEquals(2, topics.length);

        assertAnyTrue(topics, s -> "photo synthesis".equals(s.name));
        assertAnyTrue(topics, s -> "trigonometry".equals(s.name));
        assertAllTrue(topics, s -> isValidId(s.id));
    }

    @Test
    public void can_create_topic() throws Exception {
        TopicResource.CreateTopicCommand createTopicCommand = new TopicResource.CreateTopicCommand();
        createTopicCommand.name = "trigonometry";

        MockHttpServletResponse response = createResource("/topics", createTopicCommand);
        String id = getId(response);

        try (TitanTransaction transaction = graph.newTransaction()) {
            Topic topic = Topic.getById(id, transaction);
            assertEquals(createTopicCommand.name, topic.getName());
        }
    }

    @Test
    public void can_update_topic() throws Exception {
        String id;
        try (TitanTransaction transaction = graph.newTransaction()) {
            id = new Topic(transaction).getId().toString();
        }

        TopicResource.UpdateTopicCommand command = new TopicResource.UpdateTopicCommand();
        command.name = "trigonometry";

        updateResource("/topics/" + id, command);

        try (TitanTransaction transaction = graph.newTransaction()) {
            Topic topic = Topic.getById(id, transaction);
            assertEquals(command.name, topic.getName());
        }
    }

    @Test
    public void can_delete_topic() throws Exception {
        String id;
        try (TitanTransaction transaction = graph.newTransaction()) {
            id = new Topic(transaction).getId().toString();
        }

        deleteResource("/topics/" + id);
        assertNotFound(transaction -> Topic.getById(id, transaction));
    }
}
