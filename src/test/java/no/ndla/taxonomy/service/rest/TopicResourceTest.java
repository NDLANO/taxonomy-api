package no.ndla.taxonomy.service.rest;


import no.ndla.taxonomy.service.domain.Topic;
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
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
public class TopicResourceTest {

    @Autowired
    private OrientGraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_get_all_topics() throws Exception {
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            new Topic(graph).name("photo synthesis");
            new Topic(graph).name("trigonometry");
            transaction.commit();
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
        TopicResource.CreateTopicCommand createTopicCommand = new TopicResource.CreateTopicCommand() {{
            name = "trigonometry";
        }};

        MockHttpServletResponse response = createResource("/topics", createTopicCommand);
        String id = getId(response);

        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic topic = Topic.getById(id, graph);
            assertEquals(createTopicCommand.name, topic.getName());
            transaction.rollback();
        }
    }

    @Test
    public void can_create_topic_with_id() throws Exception {
        TopicResource.CreateTopicCommand createTopicCommand = new TopicResource.CreateTopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "trigonometry";
        }};

        createResource("/topics", createTopicCommand);

        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic topic = Topic.getById(createTopicCommand.id.toString(), graph);
            assertEquals(createTopicCommand.name, topic.getName());
            transaction.rollback();
        }
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        TopicResource.CreateTopicCommand command = new TopicResource.CreateTopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "name";
        }};

        createResource("/topics", command, status().isCreated());
        createResource("/topics", command, status().isConflict());
    }

    @Test
    public void can_update_topic() throws Exception {
        String id;
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            id = new Topic(graph).getId().toString();
            transaction.commit();
        }

        TopicResource.UpdateTopicCommand command = new TopicResource.UpdateTopicCommand();
        command.name = "trigonometry";

        updateResource("/topics/" + id, command);

        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic topic = Topic.getById(id, graph);
            assertEquals(command.name, topic.getName());
            transaction.rollback();
        }
    }

    @Test
    public void can_delete_topic() throws Exception {
        String id;
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            id = new Topic(graph).getId().toString();
            transaction.commit();
        }

        deleteResource("/topics/" + id);
        assertNotFound(graph -> Topic.getById(id, graph));
    }
}
