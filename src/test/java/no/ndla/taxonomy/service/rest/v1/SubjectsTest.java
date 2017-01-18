package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Subject;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SubjectsTest extends RestTest {

    @Test
    public void can_get_all_subjects() throws Exception {
        newSubject().name("english");
        newSubject().name("mathematics");

        MockHttpServletResponse response = getResource("/v1/subjects");
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

        MockHttpServletResponse response = createResource("/v1/subjects", createSubjectCommand);
        URI id = getId(response);

        Subject subject = subjectRepository.getByPublicId(id);
        assertEquals(createSubjectCommand.name, subject.getName());
    }


    @Test
    public void can_update_subject() throws Exception {
        URI id = newSubject().getPublicId();

        Subjects.UpdateSubjectCommand command = new Subjects.UpdateSubjectCommand();
        command.name = "physics";

        updateResource("/v1/subjects/" + id, command);

        Subject subject = subjectRepository.getByPublicId(id);
        assertEquals(command.name, subject.getName());
    }

    @Test
    public void can_create_subject_with_id() throws Exception {
        Subjects.CreateSubjectCommand command = new Subjects.CreateSubjectCommand() {{
            id = URI.create("urn:subject:1");
            name = "name";
        }};

        createResource("/v1/subjects", command);

        assertNotNull(subjectRepository.getByPublicId(command.id));
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        Subjects.CreateSubjectCommand command = new Subjects.CreateSubjectCommand() {{
            id = URI.create("urn:subject:1");
            name = "name";
        }};

        createResource("/v1/subjects", command, status().isCreated());
        createResource("/v1/subjects", command, status().isConflict());
    }

    @Test
    public void can_delete_subject() throws Exception {
        URI id = newSubject().getPublicId();
        deleteResource("/v1/subjects/" + id);
        assertNotFound(graph -> subjectRepository.getByPublicId(id));
    }

    @Test
    public void can_get_topics() throws Exception {
        Subject subject = newSubject().name("physics");
        save(subject.addTopic(newTopic().name("statics")));
        save(subject.addTopic(newTopic().name("electricity")));
        save(subject.addTopic(newTopic().name("optics")));

        MockHttpServletResponse response = getResource("/v1/subjects/" + subject.getPublicId() + "/topics");
        Subjects.TopicIndexDocument[] topics = getObject(Subjects.TopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(topics, t -> "statics".equals(t.name));
        assertAnyTrue(topics, t -> "electricity".equals(t.name));
        assertAnyTrue(topics, t -> "optics".equals(t.name));
        assertAllTrue(topics, t -> isValidId(t.id));
    }

    /*
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

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectid + "/topics?recursive=true");
        Subjects.TopicIndexDocument[] topics = getObject(Subjects.TopicIndexDocument[].class, response);

        assertEquals(1, topics.length);
        assertEquals("parent topic", topics[0].name);
        assertEquals("child topic", topics[0].subtopics[0].name);
        assertEquals("grandchild topic", topics[0].subtopics[0].subtopics[0].name);
    }
*/

}
