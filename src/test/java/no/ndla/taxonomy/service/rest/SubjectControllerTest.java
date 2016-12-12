package no.ndla.taxonomy.service.rest;


import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Subject;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
public class SubjectControllerTest {

    @Autowired
    private GraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_get_all_subjects() throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            new Subject(graph).name("english");
            new Subject(graph).name("mathematics");
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/subjects");
        SubjectController.SubjectIndexDocument[] subjects = getObject(SubjectController.SubjectIndexDocument[].class, response);
        assertEquals(2, subjects.length);

        assertAnyTrue(subjects, s -> "english".equals(s.name));
        assertAnyTrue(subjects, s -> "mathematics".equals(s.name));
        assertAllTrue(subjects, s -> isValidId(s.id));
    }

    @Test
    public void can_create_subject() throws Exception {
        SubjectController.CreateSubjectCommand createSubjectCommand = new SubjectController.CreateSubjectCommand();
        createSubjectCommand.name = "testsubject";

        MockHttpServletResponse response = createResource("/subjects", createSubjectCommand);
        String id = getId(response);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject subject = Subject.getById(id, graph);
            assertEquals(createSubjectCommand.name, subject.getName());
            transaction.rollback();
        }
    }


    @Test
    public void can_update_subject() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Subject(graph).getId().toString();
            transaction.commit();
        }

        SubjectController.UpdateSubjectCommand command = new SubjectController.UpdateSubjectCommand();
        command.name = "physics";

        updateResource("/subjects/" + id, command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject subject = Subject.getById(id, graph);
            assertEquals(command.name, subject.getName());
            transaction.rollback();
        }
    }

    @Test
    public void can_create_subject_with_id() throws Exception {
        SubjectController.CreateSubjectCommand command = new SubjectController.CreateSubjectCommand() {{
            id = URI.create("urn:subject:1");
            name = "name";
        }};

        createResource("/subjects", command);

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            assertNotNull(Subject.getById(command.id.toString(), graph));
            transaction.rollback();
        }
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        SubjectController.CreateSubjectCommand command = new SubjectController.CreateSubjectCommand() {{
            id = URI.create("urn:subject:1");
            name = "name";
        }};

        createResource("/subjects", command, status().isCreated());
        createResource("/subjects", command, status().isConflict());
    }

    @Test
    public void can_delete_subject() throws Exception {
        String id;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            id = new Subject(graph).getId().toString();
            transaction.commit();
        }

        deleteResource("/subjects/" + id);
        assertNotFound(graph -> Subject.getById(id, graph));
    }

    @Test
    public void can_get_topics() throws Exception {
        String subjectid;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject subject = new Subject(graph).name("physics");
            subjectid = subject.getId().toString();
            subject.addTopic(new Topic(graph).name("statics"));
            subject.addTopic(new Topic(graph).name("electricity"));
            subject.addTopic(new Topic(graph).name("optics"));
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/subjects/" + subjectid + "/topics");
        SubjectController.TopicIndexDocument[] topics = getObject(SubjectController.TopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(topics, t -> "statics".equals(t.name));
        assertAnyTrue(topics, t -> "electricity".equals(t.name));
        assertAnyTrue(topics, t -> "optics".equals(t.name));
        assertAllTrue(topics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topics_recursively() throws Exception {
        String subjectid;
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject subject = new Subject(graph).name("subject");
            subjectid = subject.getId().toString();

            Topic parent = new Topic(graph).name("parent topic");
            Topic child = new Topic(graph).name("child topic");
            Topic grandchild = new Topic(graph).name("grandchild topic");
            subject.addTopic(parent);
            parent.addSubtopic(child);
            child.addSubtopic(grandchild);
            transaction.commit();
        }

        MockHttpServletResponse response = getResource("/subjects/" + subjectid + "/topics?recursive=true");
        SubjectController.TopicIndexDocument[] topics = getObject(SubjectController.TopicIndexDocument[].class, response);

        assertEquals(1, topics.length);
        assertEquals("parent topic", topics[0].name);
        assertEquals("child topic", topics[0].subtopics[0].name);
        assertEquals("grandchild topic", topics[0].subtopics[0].subtopics[0].name);
    }
}
