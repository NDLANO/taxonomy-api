package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Subject;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SubjectsTest extends RestTest {

    @Test
    public void can_get_single_subject() throws Exception {
        URI english = builder.subject(s -> s
                .name("english")
                .contentUri("urn:article:1")
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + english);
        Subjects.SubjectIndexDocument subject = getObject(Subjects.SubjectIndexDocument.class, response);

        assertEquals("english", subject.name);
        assertEquals("urn:article:1", subject.contentUri.toString());
    }

    @Test
    public void can_get_all_subjects() throws Exception {
        builder.subject(s -> s.name("english"));
        builder.subject(s -> s.name("mathematics"));

        MockHttpServletResponse response = getResource("/v1/subjects");
        Subjects.SubjectIndexDocument[] subjects = getObject(Subjects.SubjectIndexDocument[].class, response);
        assertEquals(2, subjects.length);

        assertAnyTrue(subjects, s -> "english".equals(s.name));
        assertAnyTrue(subjects, s -> "mathematics".equals(s.name));
        assertAllTrue(subjects, s -> isValidId(s.id));
    }

    @Test
    public void can_create_subject() throws Exception {
        Subjects.CreateSubjectCommand createSubjectCommand = new Subjects.CreateSubjectCommand() {{
            name = "testsubject";
            contentUri = URI.create("urn:article:1");
        }};

        MockHttpServletResponse response = createResource("/v1/subjects", createSubjectCommand);
        URI id = getId(response);

        Subject subject = subjectRepository.getByPublicId(id);
        assertEquals(createSubjectCommand.name, subject.getName());
        assertEquals(createSubjectCommand.contentUri, subject.getContentUri());
    }

    @Test
    public void can_update_subject() throws Exception {
        URI id = builder.subject().getPublicId();

        Subjects.UpdateSubjectCommand command = new Subjects.UpdateSubjectCommand() {{
            name = "physics";
            contentUri = URI.create("urn:article:1");
        }};

        updateResource("/v1/subjects/" + id, command);

        Subject subject = subjectRepository.getByPublicId(id);
        assertEquals(command.name, subject.getName());
        assertEquals(command.contentUri, subject.getContentUri());
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
        URI id = builder.subject().getPublicId();
        deleteResource("/v1/subjects/" + id);
        assertNull(subjectRepository.findByPublicId(id));
    }

    @Test
    public void can_get_topics() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("physics")
                .topic(t -> t.name("statics").contentUri("urn:article:1"))
                .topic(t -> t.name("electricity").contentUri("urn:article:2"))
                .topic(t -> t.name("optics").contentUri("urn:article:3"))
        );

        MockHttpServletResponse response = getResource("/v1/subjects/" + subject.getPublicId() + "/topics");
        Subjects.TopicIndexDocument[] topics = getObject(Subjects.TopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(topics, t -> "statics".equals(t.name) && "urn:article:1".equals(t.contentUri.toString()));
        assertAnyTrue(topics, t -> "electricity".equals(t.name) && "urn:article:2".equals(t.contentUri.toString()));
        assertAnyTrue(topics, t -> "optics".equals(t.name) && "urn:article:3".equals(t.contentUri.toString()));
        assertAllTrue(topics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topics_with_language() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("physics")
                .topic(t -> t.name("statics").translation("nb", tr -> tr.name("statikk")))
                .topic(t -> t.name("electricity").translation("nb", tr -> tr.name("elektrisitet")))
                .topic(t -> t.name("optics").translation("nb", tr -> tr.name("optikk")))
        );

        MockHttpServletResponse response = getResource("/v1/subjects/" + subject.getPublicId() + "/topics?language=nb");
        Subjects.TopicIndexDocument[] topics = getObject(Subjects.TopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(topics, t -> "statikk".equals(t.name));
        assertAnyTrue(topics, t -> "elektrisitet".equals(t.name));
        assertAnyTrue(topics, t -> "optikk".equals(t.name));
    }

    @Test
    public void can_get_topics_recursively() throws Exception {
        URI subjectid = builder.subject(s -> s
                .name("subject")
                .topic(parent -> parent
                        .name("parent topic")
                        .subtopic(child -> child
                                .name("child topic")
                                .subtopic(grandchild -> grandchild.name("grandchild topic"))
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectid + "/topics?recursive=true");
        Subjects.TopicIndexDocument[] topics = getObject(Subjects.TopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertEquals("parent topic", topics[0].name);
        assertEquals("child topic", topics[1].name);
        assertEquals("grandchild topic", topics[2].name);
    }

    @Test
    public void resources_can_have_content_uri() throws Exception {
        URI id = builder.subject(s -> s
                .topic(t -> t
                        .resource(r -> r.contentUri("urn:article:1"))
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + id + "/resources");
        Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

        assertEquals("urn:article:1", resources[0].contentUri.toString());
    }

    @Test
    public void can_get_translated_resources() throws Exception {

        builder.resourceType("article", rt -> rt
                .name("Article")
                .translation("nb", tr -> tr.name("Artikkel"))
        );

        URI id = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("Trigonometry")
                        .resource(r -> r
                                .name("Introduction to trigonometry")
                                .translation("nb", tr -> tr.name("Introduksjon til trigonometri"))
                                .resourceType("article")
                        )
                )
                .topic(t -> t
                        .name("Calculus")
                        .resource(r -> r
                                .name("Introduction to calculus")
                                .translation("nb", tr -> tr.name("Introduksjon til calculus"))
                                .resourceType("article")
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + id + "/resources?language=nb");
        Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);

        assertAnyTrue(resources, r -> r.name.equals("Introduksjon til trigonometri"));
        assertAnyTrue(resources, r -> r.name.equals("Introduksjon til calculus"));
        assertAllTrue(resources, r -> r.resourceTypes.get(0).name.equals("Artikkel"));
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

        MockHttpServletResponse response = getResource("/v1/subjects/" + id + "/resources");
        Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

        assertEquals(3, resources.length);
    }

    @Test
    public void can_get_urls_for_all_resources() throws Exception {
        URI id = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("topic a")
                        .resource(r -> r.name("resource a")))
                .topic(t -> t
                        .name("topic b")
                        .resource(r -> r.name("resource b"))
                        .subtopic(st -> st.name("subtopic").resource(r -> r.name("sub resource"))))
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + id + "/resources");
        Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

        assertEquals(3, resources.length);
        assertAllTrue(resources, r -> r.path.contains("topic") && r.path.contains("resource"));
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

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources?type=" + lecture);
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture".equals(r.name));
    }
}
