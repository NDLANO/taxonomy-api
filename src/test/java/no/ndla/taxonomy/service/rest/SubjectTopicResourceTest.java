package no.ndla.taxonomy.service.rest;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.SubjectTopic;
import no.ndla.taxonomy.service.domain.Topic;
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
public class SubjectTopicResourceTest {

    @Autowired
    private TitanGraph graph;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_add_topic_to_subject() throws Exception {
        URI subjectId, topicId;
        try (TitanTransaction transaction = graph.newTransaction()) {
            subjectId = new Subject(transaction).name("physics").getId();
            topicId = new Topic(transaction).name("trigonometry").getId();
        }

        String id = getId(
                createResource("/subject-topics", new SubjectTopicResource.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }})
        );

        try (TitanTransaction transaction = graph.newTransaction()) {
            Subject physics = Subject.getById(subjectId.toString(), transaction);
            assertEquals(1, count(physics.getTopics()));
            assertAnyTrue(physics.getTopics(), t -> "trigonometry".equals(t.getName()));
            assertNotNull(SubjectTopic.getById(id, transaction));
        }
    }

    @Test
    public void canot_add_existing_topic_to_subject() throws Exception {
        URI subjectId, topicId;

        try (TitanTransaction transaction = graph.newTransaction()) {
            Subject physics = new Subject(transaction).name("physics");
            Topic trigonometry = new Topic(transaction).name("trigonometry");
            physics.addTopic(trigonometry);

            subjectId = physics.getId();
            topicId = trigonometry.getId();
        }

        createResource("/subject-topics", new SubjectTopicResource.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }},
                status().isConflict()
        );
    }


    @Test
    public void can_delete_subject_topic() throws Exception {
        String id;
        try (TitanTransaction transaction = graph.newTransaction()) {
            id = new Subject(transaction).addTopic(new Topic(transaction)).getId().toString();
        }

        deleteResource("/subject-topics/" + id);
        assertNotFound(transaction -> Subject.getById(id, transaction));
    }

    @Test
    public void can_update_subject_topic() throws Exception {
        String id;
        try (TitanTransaction transaction = graph.newTransaction()) {
            id = new Subject(transaction).addTopic(new Topic(transaction)).getId().toString();
        }

        SubjectTopicResource.UpdateSubjectTopicCommand command = new SubjectTopicResource.UpdateSubjectTopicCommand();
        command.primary = true;

        updateResource("/subject-topics/" + id, command);

        try (TitanTransaction transaction = graph.newTransaction()) {
            assertTrue(SubjectTopic.getById(id, transaction).isPrimary());
        }
    }

    @Test
    public void can_get_topics() throws Exception {
        URI physicsId, electricityId, mathematicsId, trigonometryId;

        try (TitanTransaction transaction = graph.newTransaction()) {
            Subject physics = new Subject(transaction).name("physics");
            Topic electricity = new Topic(transaction).name("electricity");
            physics.addTopic(electricity);

            Subject mathematics = new Subject(transaction).name("mathematics");
            Topic trigonometry = new Topic(transaction).name("trigonometry");
            mathematics.addTopic(trigonometry);

            physicsId = physics.getId();
            electricityId = electricity.getId();
            mathematicsId = mathematics.getId();
            trigonometryId = trigonometry.getId();
        }

        MockHttpServletResponse response = getResource("/subject-topics");
        SubjectTopicResource.SubjectTopicIndexDocument[] subjectTopics = getObject(SubjectTopicResource.SubjectTopicIndexDocument[].class, response);

        assertEquals(2, subjectTopics.length);
        assertAnyTrue(subjectTopics, t -> physicsId.equals(t.subjectid) && electricityId.equals(t.topicid));
        assertAnyTrue(subjectTopics, t -> mathematicsId.equals(t.subjectid) && trigonometryId.equals(t.topicid));
        assertAllTrue(subjectTopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_subject_topic() throws Exception {
        URI topicid, subjectid, id;
        try (TitanTransaction transaction = graph.newTransaction()) {
            Subject physics = new Subject(transaction).name("physics");
            Topic electricity = new Topic(transaction).name("electricity");
            SubjectTopic subjectTopic = physics.addTopic(electricity);

            subjectid = physics.getId();
            topicid = electricity.getId();
            id = subjectTopic.getId();
        }

        MockHttpServletResponse resource = getResource("/subject-topics/" + id);
        SubjectTopicResource.SubjectTopicIndexDocument subjectTopicIndexDocument = getObject(SubjectTopicResource.SubjectTopicIndexDocument.class, resource);
        assertEquals(subjectid, subjectTopicIndexDocument.subjectid);
        assertEquals(topicid, subjectTopicIndexDocument.topicid);
    }
}
