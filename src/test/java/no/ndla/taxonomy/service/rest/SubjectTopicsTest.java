package no.ndla.taxonomy.service.rest;


import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.SubjectTopic;
import no.ndla.taxonomy.service.domain.Topic;
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
public class SubjectTopicsTest {

    @Autowired
    private GraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_add_topic_to_subject() throws Exception {
        URI subjectId, topicId;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            subjectId = new Subject(graph).name("physics").getId();
            topicId = new Topic(graph).name("trigonometry").getId();
            transaction.commit();
        }

        String id = getId(
                createResource("/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }})
        );

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject physics = Subject.getById(subjectId.toString(), graph);
            assertEquals(1, count(physics.getTopics()));
            assertAnyTrue(physics.getTopics(), t -> "trigonometry".equals(t.getName()));
            assertNotNull(SubjectTopic.getById(id, graph));
            transaction.rollback();
        }
    }

    @Test
    public void canot_add_existing_topic_to_subject() throws Exception {
        URI subjectId, topicId;

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject physics = new Subject(graph).name("physics");
            Topic trigonometry = new Topic(graph).name("trigonometry");
            physics.addTopic(trigonometry);

            subjectId = physics.getId();
            topicId = trigonometry.getId();
            transaction.commit();
        }

        createResource("/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }},
                status().isConflict()
        );
    }


    @Test
    public void can_delete_subject_topic() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Subject(graph).addTopic(new Topic(graph)).getId().toString();
            transaction.commit();
        }

        deleteResource("/subject-topics/" + id);
        assertNotFound(graph -> Subject.getById(id, graph));
    }

    @Test
    public void can_update_subject_topic() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Subject(graph).addTopic(new Topic(graph)).getId().toString();
            transaction.commit();
        }

        SubjectTopics.UpdateSubjectTopicCommand command = new SubjectTopics.UpdateSubjectTopicCommand();
        command.primary = true;

        updateResource("/subject-topics/" + id, command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            assertTrue(SubjectTopic.getById(id, graph).isPrimary());
            transaction.rollback();
        }
    }

    @Test
    public void can_get_topics() throws Exception {
        URI physicsId, electricityId, mathematicsId, trigonometryId;

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject physics = new Subject(graph).name("physics");
            Topic electricity = new Topic(graph).name("electricity");
            physics.addTopic(electricity);

            Subject mathematics = new Subject(graph).name("mathematics");
            Topic trigonometry = new Topic(graph).name("trigonometry");
            mathematics.addTopic(trigonometry);

            physicsId = physics.getId();
            electricityId = electricity.getId();
            mathematicsId = mathematics.getId();
            trigonometryId = trigonometry.getId();
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/subject-topics");
        SubjectTopics.SubjectTopicIndexDocument[] subjectTopics = getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertEquals(2, subjectTopics.length);
        assertAnyTrue(subjectTopics, t -> physicsId.equals(t.subjectid) && electricityId.equals(t.topicid));
        assertAnyTrue(subjectTopics, t -> mathematicsId.equals(t.subjectid) && trigonometryId.equals(t.topicid));
        assertAllTrue(subjectTopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_subject_topic() throws Exception {
        URI topicid, subjectid, id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject physics = new Subject(graph).name("physics");
            Topic electricity = new Topic(graph).name("electricity");
            SubjectTopic subjectTopic = physics.addTopic(electricity);

            subjectid = physics.getId();
            topicid = electricity.getId();
            id = subjectTopic.getId();
            transaction.commit();
        }

        MockHttpServletResponse resource = getResource("/subject-topics/" + id);
        SubjectTopics.SubjectTopicIndexDocument subjectTopicIndexDocument = getObject(SubjectTopics.SubjectTopicIndexDocument.class, resource);
        assertEquals(subjectid, subjectTopicIndexDocument.subjectid);
        assertEquals(topicid, subjectTopicIndexDocument.topicid);
    }
}
