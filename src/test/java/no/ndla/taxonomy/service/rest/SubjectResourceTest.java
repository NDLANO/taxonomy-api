package no.ndla.taxonomy.service.rest;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.Subject;
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
public class SubjectResourceTest {

    @Autowired
    private TitanGraph graph;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_get_all_subjects() throws Exception {
        try (TitanTransaction transaction = graph.newTransaction()) {
            new Subject(transaction).name("english");
            new Subject(transaction).name("mathematics");
        }

        MockHttpServletResponse response = getResource("/subjects");
        SubjectResource.SubjectIndexDocument[] subjects = getObject(SubjectResource.SubjectIndexDocument[].class, response);
        assertEquals(2, subjects.length);

        assertAnyTrue(subjects, s -> "english".equals(s.name));
        assertAnyTrue(subjects, s -> "mathematics".equals(s.name));
        assertAllTrue(subjects, s -> isValidId(s.id));
    }

    @Test
    public void can_create_subject() throws Exception {
        SubjectResource.CreateSubjectCommand createSubjectCommand = new SubjectResource.CreateSubjectCommand();
        createSubjectCommand.name = "testsubject";

        MockHttpServletResponse response = createResource("/subjects", createSubjectCommand);
        String id = getId(response);

        try (TitanTransaction transaction = graph.newTransaction()) {
            Subject subject = Subject.getById(id, transaction);
            assertEquals(createSubjectCommand.name, subject.getName());
        }
    }


    @Test
    public void can_update_subject() throws Exception {
        String id;
        try (TitanTransaction transaction = graph.newTransaction()) {
            id = new Subject(transaction).getId().toString();
        }

        SubjectResource.UpdateSubjectCommand command = new SubjectResource.UpdateSubjectCommand();
        command.name = "physics";

        updateResource("/subjects/" + id, command);

        try (TitanTransaction transaction = graph.newTransaction()) {
            Subject subject = Subject.getById(id, transaction);
            assertEquals(command.name, subject.getName());
        }
    }


    @Test
    public void can_delete_subject() throws Exception {
        String id;
        try (TitanTransaction transaction = graph.newTransaction()) {
            id = new Subject(transaction).getId().toString();
        }

        deleteResource("/subjects/" + id);
        assertNotFound(transaction -> Subject.getById(id, transaction));
    }

    @Test
    public void can_get_topics() throws Exception {
        String subjectid;
        try (TitanTransaction transaction = graph.newTransaction()) {
            Subject subject = new Subject(transaction).name("physics");
            subjectid = subject.getId().toString();
            subject.addTopic(new Topic(transaction).name("statics"));
            subject.addTopic(new Topic(transaction).name("electricity"));
            subject.addTopic(new Topic(transaction).name("optics"));
        }

        MockHttpServletResponse response = getResource("/subjects/" + subjectid + "/topics");
        SubjectResource.TopicIndexDocument[] topics = getObject(SubjectResource.TopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(topics, t -> "statics".equals(t.name));
        assertAnyTrue(topics, t -> "electricity".equals(t.name));
        assertAnyTrue(topics, t -> "optics".equals(t.name));
        assertAllTrue(topics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topics_recursively() throws Exception {
        String subjectid;
        try (TitanTransaction transaction = graph.newTransaction()) {
            Subject subject = new Subject(transaction).name("subject");
            subjectid = subject.getId().toString();

            Topic parent = new Topic(transaction).name("parent topic");
            Topic child = new Topic(transaction).name("child topic");
            Topic grandchild = new Topic(transaction).name("grandchild topic");
            subject.addTopic(parent);
            parent.addSubtopic(child);
            child.addSubtopic(grandchild);
        }

        MockHttpServletResponse response = getResource("/subjects/" + subjectid + "/topics?recursive=true");
        SubjectResource.TopicIndexDocument[] topics = getObject(SubjectResource.TopicIndexDocument[].class, response);

        assertEquals(1, topics.length);
        assertEquals("parent topic", topics[0].name);
        assertEquals("child topic", topics[0].subtopics[0].name);
        assertEquals("grandchild topic", topics[0].subtopics[0].subtopics[0].name);
    }
}
