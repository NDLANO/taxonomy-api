package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.*;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SubjectsTest extends RestTest {

    @Test
    public void can_get_single_subject() throws Exception {
        URI english = builder.subject(s -> s
                .name("english")
                .contentUri("urn:article:1")
                .publicId("urn:subject:1")
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + english);
        Subjects.SubjectIndexDocument subject = getObject(Subjects.SubjectIndexDocument.class, response);

        assertEquals("english", subject.name);
        assertEquals("urn:article:1", subject.contentUri.toString());
        assertEquals("/subject:1", subject.path);
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
        assertAllTrue(subjects, s -> !s.path.isEmpty());
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

        MockHttpServletResponse response = createResource("/v1/subjects", command);
        assertEquals("/v1/subjects/urn:subject:1", response.getHeader("Location"));

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
        URI id = builder.subject(s -> s
                .topic(t -> t.publicId("urn:topic:1"))
                .translation("nb", tr -> tr.name("fag"))
                .filter(f -> f.publicId("urn:filter:1"))
        ).getPublicId();
        deleteResource("/v1/subjects/" + id);
        assertNull(subjectRepository.findByPublicId(id));
        assertNull(filterRepository.findByPublicId(URI.create("urn:filter:1")));
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
        Subjects.SubTopicIndexDocument[] topics = getObject(Subjects.SubTopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(topics, t -> "statics".equals(t.name) && "urn:article:1".equals(t.contentUri.toString()));
        assertAnyTrue(topics, t -> "electricity".equals(t.name) && "urn:article:2".equals(t.contentUri.toString()));
        assertAnyTrue(topics, t -> "optics".equals(t.name) && "urn:article:3".equals(t.contentUri.toString()));
        assertAnyTrue(topics, t -> t.isPrimary);
        assertAllTrue(topics, t -> isValidId(t.id));
        assertAllTrue(topics, t -> isValidId(t.connectionId));
        assertAllTrue(topics, t -> t.parent.equals(subject.getPublicId()));
    }


    @Test
    public void can_get_topics_recursively() throws Exception {
        URI subjectid = builder.subject("subject", s -> s
                .name("subject")
                .publicId("urn:subject:1")
                .topic("parent", parent -> parent
                        .name("parent topic")
                        .publicId("urn:topic:a")
                        .subtopic("child", child -> child
                                .name("child topic")
                                .publicId("urn:topic:aa")
                                .subtopic("grandchild", grandchild -> grandchild
                                        .name("grandchild topic")
                                        .publicId("urn:topic:aaa")
                                )
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectid + "/topics?recursive=true");
        Subjects.SubTopicIndexDocument[] topics = getObject(Subjects.SubTopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertEquals("parent topic", topics[0].name);
        assertEquals("child topic", topics[1].name);
        assertEquals("grandchild topic", topics[2].name);

        Subject subject = builder.subject("subject");
        assertEquals(first(subject.topics).getPublicId(), topics[0].connectionId);

        Topic parent = builder.topic("parent");
        assertEquals(first(parent.subtopics).getPublicId(), topics[1].connectionId);

        Topic child = builder.topic("child");
        assertEquals(first(child.subtopics).getPublicId(), topics[2].connectionId);
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
    public void resources_can_have_filters() throws Exception {
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core"));
        Filter filter = builder.filter(f -> f.publicId("urn:filter:vg1"));

        URI id = builder.subject(s -> s
                .topic(t -> t
                        .resource(r -> r
                                .contentUri("urn:article:1")
                                .filter(filter, relevance)
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + id + "/resources");
        Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

        assertEquals("urn:filter:vg1", first(resources[0].filters).id.toString());
        assertEquals("urn:relevance:core", first(resources[0].filters).relevanceId.toString());
    }

    @Test
    public void can_get_resources_for_a_subject_and_its_topics_recursively() throws Exception {
        URI id = builder.subject(s -> s
                .name("subject")
                .topic("topic a", t -> t
                        .name("topic a")
                        .resource(r -> r.name("resource a").resourceType(rt -> rt.name("assignment"))))
                .topic("topic b", t -> t
                        .name("topic b")
                        .resource(r -> r.name("resource b").resourceType(rt -> rt.name("lecture")))
                        .subtopic("subtopic", st -> st.name("subtopic").resource(r -> r.name("sub resource"))))
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + id + "/resources");
        Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

        assertEquals(3, resources.length);

        assertAnyTrue(resources, r -> r.connectionId.equals(first(builder.topic("topic a").resources).getPublicId()));
        assertAnyTrue(resources, r -> r.connectionId.equals(first(builder.topic("topic b").resources).getPublicId()));
        assertAnyTrue(resources, r -> r.connectionId.equals(first(builder.topic("subtopic").resources).getPublicId()));
    }

    @Test
    public void can_get_urls_for_all_resources() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r.publicId("urn:resource:1"))
                )
                .topic(t -> t
                        .publicId("urn:topic:2")
                        .resource(r -> r.publicId("urn:resource:2"))
                        .subtopic(st -> st
                                .publicId("urn:topic:21")
                                .resource(r -> r.publicId("urn:resource:3"))
                        )
                )
        );

        MockHttpServletResponse response = getResource("/v1/subjects/urn:subject:1/resources");
        Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

        assertEquals(3, resources.length);
        assertAnyTrue(resources, r -> r.path.equals("/subject:1/topic:1/resource:1"));
        assertAnyTrue(resources, r -> r.path.equals("/subject:1/topic:2/resource:2"));
        assertAnyTrue(resources, r -> r.path.equals("/subject:1/topic:2/topic:21/resource:3"));
    }

    @Test
    public void resource_urls_are_chosen_according_to_context() throws Exception {
        Resource resource = builder.resource(r -> r.publicId("urn:resource:1"));

        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(resource)
                )
        );
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic("topic2", t -> t
                        .publicId("urn:topic:2")
                        .resource(resource)
                )
        );

        for (int i : asList(1, 2)) {
            MockHttpServletResponse response = getResource("/v1/subjects/urn:subject:" + i + "/resources");
            Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

            assertEquals(1, resources.length);
            assertEquals("/subject:" + i + "/topic:" + i + "/resource:1", resources[0].path);
        }
    }

    @Test
    public void topic_urls_are_chosen_according_to_context() throws Exception {
        Topic topic = builder.topic(t -> t.publicId("urn:topic:1"));

        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(topic)
        );
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(topic)
        );

        for (int i : asList(1, 2)) {
            MockHttpServletResponse response = getResource("/v1/subjects/urn:subject:" + i + "/topics");
            Subjects.SubTopicIndexDocument[] resources = getObject(Subjects.SubTopicIndexDocument[].class, response);

            assertEquals(1, resources.length);
        }
    }
}
