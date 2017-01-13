package no.ndla.taxonomy.service.rest.v1;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
public class TopicSubtopicsTest {
    @Test
    public void name() throws Exception {


    }
/*
    @Autowired
    private GraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_add_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            calculusId = new Topic(graph).name("calculus").getId();
            integrationId = new Topic(graph).name("integration").getId();
            transaction.commit();
        }

        String id = getId(
                createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }})
        );

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
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
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Topic calculus = new Topic(graph).name("calculus");
            Topic integration = new Topic(graph).name("integration");
            calculus.addSubtopic(integration);

            calculusId = calculus.getId();
            integrationId = integration.getId();
            transaction.commit();
        }

        createResource("/v1/topic-subtopics",
                new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }},
                status().isConflict()
        );
    }


    @Test
    public void can_delete_subtopic_topic() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Topic(graph).addSubtopic(new Topic(graph)).getId().toString();
            transaction.commit();
        }

        deleteResource("/v1/topic-subtopics/" + id);
        assertNotFound(graph -> Topic.getById(id, graph));
    }

    @Test
    public void can_update_topic_subtopic() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Topic(graph).addSubtopic(new Topic(graph)).getId().toString();
            transaction.commit();
        }

        TopicSubtopics.UpdateTopicSubtopicCommand command = new TopicSubtopics.UpdateTopicSubtopicCommand();
        command.primary = true;

        updateResource("/v1/topic-subtopics/" + id, command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            assertTrue(TopicSubtopic.getById(id, graph).isPrimary());
            transaction.rollback();
        }
    }

    @Test
    public void can_get_topics() throws Exception {
        URI alternatingCurrentId, electricityId, calculusId, integrationId;

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
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

        MockHttpServletResponse response = getResource("/v1/topic-subtopics");
        TopicSubtopics.TopicSubtopicIndexDocument[] topicSubtopics = getObject(TopicSubtopics.TopicSubtopicIndexDocument[].class, response);

        assertEquals(2, topicSubtopics.length);
        assertAnyTrue(topicSubtopics, t -> electricityId.equals(t.topicid) && alternatingCurrentId.equals(t.subtopicid));
        assertAnyTrue(topicSubtopics, t -> calculusId.equals(t.topicid) && integrationId.equals(t.subtopicid));
        assertAllTrue(topicSubtopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topic_subtopic() throws Exception {
        URI topicid, subtopicid, id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Topic electricity = new Topic(graph).name("electricity");
            Topic alternatingCurrent = new Topic(graph).name("alternating current");
            TopicSubtopic topicSubtopic = electricity.addSubtopic(alternatingCurrent);

            topicid = electricity.getId();
            subtopicid = alternatingCurrent.getId();
            id = topicSubtopic.getId();
            transaction.commit();
        }

        MockHttpServletResponse resource = getResource("/v1/topic-subtopics/" + id);
        TopicSubtopics.TopicSubtopicIndexDocument topicSubtopicIndexDocument = getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, resource);
        assertEquals(topicid, topicSubtopicIndexDocument.topicid);
        assertEquals(subtopicid, topicSubtopicIndexDocument.subtopicid);
    }
    */
}
