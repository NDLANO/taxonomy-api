package no.ndla.taxonomy.service.rest;


import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.repositories.SubjectRepository;
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
public class SubjectsTest {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private GraphFactory factory;

    @Before
    public void setup() throws Exception {
        clearGraph();
    }

    @Test
    public void can_get_all_subjects() throws Exception {
        subjectRepository.save(new Subject().name("english"));
        subjectRepository.save(new Subject().name("mathematics"));

        MockHttpServletResponse response = getResource("/subjects");
        Subjects.SubjectIndexDocument[] subjects = getObject(Subjects.SubjectIndexDocument[].class, response);
        assertEquals(2, subjects.length);

        assertAnyTrue(subjects, s -> "english".equals(s.name));
        assertAnyTrue(subjects, s -> "mathematics".equals(s.name));
        assertAllTrue(subjects, s -> isValidId(s.id));
    }

    @Test
    public void can_create_subject() throws Exception {
        Subjects.CreateSubjectCommand createSubjectCommand = new Subjects.CreateSubjectCommand();
        createSubjectCommand.name = "testsubject";

        MockHttpServletResponse response = createResource("/subjects", createSubjectCommand);
        String id = getId(response);


        Subject subject = subjectRepository.getById(id);
        assertEquals(createSubjectCommand.name, subject.getName());
    }


    @Test
    public void can_update_subject() throws Exception {
        URI id = subjectRepository.save(new Subject()).getId();

        Subjects.UpdateSubjectCommand command = new Subjects.UpdateSubjectCommand();
        command.name = "physics";

        updateResource("/subjects/" + id, command);

        Subject subject = subjectRepository.getById(id);
        assertEquals(command.name, subject.getName());
    }

    @Test
    public void can_create_subject_with_id() throws Exception {
        Subjects.CreateSubjectCommand command = new Subjects.CreateSubjectCommand() {{
            id = URI.create("urn:subject:1");
            name = "name";
        }};

        createResource("/subjects", command);

        assertNotNull(subjectRepository.getById(command.id));
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        Subjects.CreateSubjectCommand command = new Subjects.CreateSubjectCommand() {{
            id = URI.create("urn:subject:1");
            name = "name";
        }};

        createResource("/subjects", command, status().isCreated());
        createResource("/subjects", command, status().isConflict());
    }

    @Test
    public void can_delete_subject() throws Exception {
        URI id = subjectRepository.save(new Subject()).getId();
        deleteResource("/subjects/" + id);
        assertNotFound(graph -> subjectRepository.getById(id));
    }

    /*
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
        Subjects.TopicIndexDocument[] topics = getObject(Subjects.TopicIndexDocument[].class, response);

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
        Subjects.TopicIndexDocument[] topics = getObject(Subjects.TopicIndexDocument[].class, response);

        assertEquals(1, topics.length);
        assertEquals("parent topic", topics[0].name);
        assertEquals("child topic", topics[0].subtopics[0].name);
        assertEquals("grandchild topic", topics[0].subtopics[0].subtopics[0].name);
    }

    */
}
