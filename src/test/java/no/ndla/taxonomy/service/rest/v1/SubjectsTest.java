package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.Topic;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
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
        assertNull(subjectRepository.findByPublicId(id));
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

    @Test
    public void can_get_topics_recursively() throws Exception {
        Subject subject = newSubject().name("subject");
        URI subjectid = subject.getPublicId();

        Topic parent = newTopic().name("parent topic");
        Topic child = newTopic().name("child topic");
        Topic grandchild = newTopic().name("grandchild topic");
        save(subject.addTopic(parent));
        save(parent.addSubtopic(child));
        save(child.addSubtopic(grandchild));

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectid + "/topics?recursive=true");
        Subjects.TopicIndexDocument[] topics = getObject(Subjects.TopicIndexDocument[].class, response);

        assertEquals(1, topics.length);
        assertEquals("parent topic", topics[0].name);
        assertEquals("child topic", topics[0].subtopics[0].name);
        assertEquals("grandchild topic", topics[0].subtopics[0].subtopics[0].name);
    }

    @Test
    public void can_get_resources_for_a_subject_and_its_topics_recursively() throws Exception {
        URI id = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("topic a")
                        .resource(r -> r.name("resource a").resourceType(rt -> rt.name("assignment"))))
                .topic(t -> t
                        .name("topic b")
                        .resource(r -> r.name("resource b").resourceType(rt -> rt.name("lecture")))
                        .subtopic(st -> st.name("subtopic").resource(r -> r.name("sub resource"))))
        ).getPublicId();
        flush();

        MockHttpServletResponse response = getResource("/v1/subjects/" + id + "/resources");
        Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

        assertEquals(3, resources.length);

    }

    @Test
    public void can_have_several_resource_types_recursively() throws Exception {
        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("topic")
                        .subtopic(st -> st
                                .resource(r -> r
                                        .name("resource 1")
                                        .resourceType(rt -> rt.name("lecture"))
                                        .resourceType(rt -> rt.name("assignment"))
                                )
                        )
                        .resource(r -> r
                                .name("resource 2")
                                .resourceType(rt -> rt.name("lecture"))
                                .resourceType(rt -> rt.name("assignment"))
                        )
                )
        ).getPublicId();
        flush();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertEquals(2, result[0].resourceTypes.size());
        assertEquals(2, result[1].resourceTypes.size());
    }

    @Test
    public void can_have_no_resource_type() throws Exception {
        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("topic")
                        .subtopic(st -> st
                                .resource(r -> r
                                        .name("resource 1")
                                )
                        )
                )
        ).getPublicId();
        flush();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals(0, result[0].resourceTypes.size());
    }

    @Test
    public void can_get_resources_for_a_subject_filtered_on_resource_type() throws Exception {
        builder.resourceType("assignment").getPublicId();
        URI lecture = builder.resourceType("lecture").getPublicId();

        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("a")
                        .subtopic(sub -> sub.name("subtopic").resource(r -> r.name("a lecture in a subtopic").resourceType("lecture")))
                        .resource(r -> r.name("an assignment").resourceType("assignment"))
                        .resource(r -> r.name("a lecture").resourceType("lecture"))
                )
        ).getPublicId();

        flush();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources?type=" + lecture);
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture".equals(r.name));
    }
}
