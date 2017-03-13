package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.ResourceType;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.repositories.TopicRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicsTest extends RestTest {

    @Autowired
    private TopicRepository topicRepository;

    @Test
    public void can_get_single_topic() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .name("trigonometry")
                        .contentUri("urn:article:1")
                        .publicId("urn:topic:1")
                ));


        MockHttpServletResponse response = getResource("/v1/topics/" + "urn:topic:1");
        Topics.TopicIndexDocument topic = getObject(Topics.TopicIndexDocument.class, response);

        assertEquals("trigonometry", topic.name);
        assertEquals("urn:article:1", topic.contentUri.toString());
        assertEquals("/subject:1/topic:1", topic.path);
    }

    @Test
    public void primary_url_is_returned_when_getting_single_topic() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic("topic", t -> t.publicId("urn:topic:1"))
        );

        Subject primary = builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic("topic")
        );
        builder.topic("topic").setPrimarySubject(primary);

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1");
        Topics.TopicIndexDocument topic = getObject(Topics.TopicIndexDocument.class, response);

        assertEquals("/subject:2/topic:1", topic.path);
    }

    @Test
    public void topic_without_subject_has_no_url() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
        );

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1");
        Topics.TopicIndexDocument topic = getObject(Topics.TopicIndexDocument.class, response);

        assertNull(topic.path);
    }

    @Test
    public void can_get_all_topics() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(t -> t.name("photo synthesis")));
        builder.subject(s -> s
                .name("Maths")
                .topic(t -> t.name("trigonometry")));

        MockHttpServletResponse response = getResource("/v1/topics");
        Topics.TopicIndexDocument[] topics = getObject(Topics.TopicIndexDocument[].class, response);
        assertEquals(2, topics.length);

        assertAnyTrue(topics, s -> "photo synthesis".equals(s.name));
        assertAnyTrue(topics, s -> "trigonometry".equals(s.name));
        assertAllTrue(topics, s -> isValidId(s.id));
        assertAllTrue(topics, t -> t.path.contains("subject") && t.path.contains("topic"));
    }

    @Test
    public void can_create_topic() throws Exception {
        Topics.CreateTopicCommand createTopicCommand = new Topics.CreateTopicCommand() {{
            name = "trigonometry";
            contentUri = URI.create("urn:article:1");
        }};

        MockHttpServletResponse response = createResource("/v1/topics", createTopicCommand);
        URI id = getId(response);

        Topic topic = topicRepository.getByPublicId(id);
        assertEquals(createTopicCommand.name, topic.getName());
        assertEquals(createTopicCommand.contentUri, topic.getContentUri());
    }

    @Test
    public void can_create_topic_with_id() throws Exception {
        Topics.CreateTopicCommand createTopicCommand = new Topics.CreateTopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "trigonometry";
        }};

        createResource("/v1/topics", createTopicCommand);

        Topic topic = topicRepository.getByPublicId(createTopicCommand.id);
        assertEquals(createTopicCommand.name, topic.getName());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        Topics.CreateTopicCommand command = new Topics.CreateTopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "name";
        }};

        createResource("/v1/topics", command, status().isCreated());
        createResource("/v1/topics", command, status().isConflict());
    }

    @Test
    public void can_update_topic() throws Exception {
        URI id = builder.topic().getPublicId();

        updateResource("/v1/topics/" + id, new Topics.UpdateTopicCommand() {{
            name = "trigonometry";
            contentUri = URI.create("urn:article:1");
        }});

        Topic topic = topicRepository.getByPublicId(id);
        assertEquals("trigonometry", topic.getName());
        assertEquals("urn:article:1", topic.getContentUri().toString());
    }

    @Test
    public void can_delete_topic() throws Exception {
        builder.topic(parent -> parent
                .subtopic("topic", topic -> topic
                        .name("DELETE ME")
                        .translation("nb", tr -> tr.name("emne"))
                        .subtopic(sub -> sub.publicId("urn:topic:2"))
                        .resource(r -> r.publicId("urn:resource:1"))
                )
        );
        builder.subject(s -> s.topic("topic"));
        URI id = builder.topic("topic").getPublicId();

        deleteResource("/v1/topics/" + id);

        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_get_resources_for_a_topic_recursively() throws Exception {
        builder.subject(s -> s
                .name("subject a")
                .topic(t -> t
                        .name("a")
                        .publicId("urn:topic:a")
                        .resource(r -> r.name("resource a").contentUri("urn:article:a"))
                        .subtopic(st -> st
                                .name("aa")
                                .resource(r -> r.name("resource aa").contentUri("urn:article:aa"))
                                .subtopic(st2 -> st2
                                        .name("aaa")
                                        .resource(r -> r.name("resource aaa").contentUri("urn:article:aaa"))
                                )
                                .subtopic(st2 -> st2
                                        .name("aab")
                                        .resource(r -> r.name("resource aab").contentUri("urn:article:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = getResource("/v1/topics/" + "urn:topic:a" + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.name) && "urn:article:a".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.name) && "urn:article:aa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aaa".equals(r.name) && "urn:article:aaa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aab".equals(r.name) && "urn:article:aab".equals(r.contentUri.toString()));
        assertAllTrue(result, r -> !r.path.isEmpty());
    }

    @Test
    public void can_get_urls_for_resources_for_a_topic_recursively() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:a")
                        .resource(r -> r.publicId("urn:resource:a"))
                        .subtopic(st -> st
                                .publicId("urn:topic:aa")
                                .resource(r -> r.publicId("urn:resource:aa"))
                                .subtopic(st2 -> st2
                                        .publicId("urn:topic:aaa")
                                        .resource(r -> r.publicId("urn:resource:aaa"))
                                )
                                .subtopic(st2 -> st2
                                        .publicId("urn:topic:aab")
                                        .resource(r -> r.publicId("urn:resource:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = getResource("/v1/topics/" + "urn:topic:a" + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(result, r -> "/subject:1/topic:a/resource:a".equals(r.path));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/resource:aa".equals(r.path));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/topic:aaa/resource:aaa".equals(r.path));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/topic:aab/resource:aab".equals(r.path));
    }

    @Test
    public void can_get_resources_for_a_topic_without_child_topic_resources() throws Exception {
        builder.subject(s -> s
                .topic(t -> t
                        .name("a")
                        .publicId("urn:topic:1")
                        .subtopic(st -> st.name("subtopic").resource(r -> r.name("subtopic resource")))
                        .resource(r -> r.name("resource 1"))
                        .resource(r -> r.name("resource 2"))
                ));

        MockHttpServletResponse response = getResource("/v1/topics/" + "urn:topic:1" + "/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "resource 1".equals(r.name));
        assertAnyTrue(result, r -> "resource 2".equals(r.name));
    }

    @Test
    public void can_get_resources_for_a_topic_filtered_on_resource_type() throws Exception {
        URI assignment = builder.resourceType("assignment").getPublicId();
        URI lecture = builder.resourceType("lecture").getPublicId();

        builder.subject(s -> s.topic(t -> t
                .name("a")
                .publicId("urn:topic:1")
                .subtopic(sub -> sub.name("subtopic").resource(r -> r.name("a lecture in a subtopic").resourceType("lecture")))
                .resource(r -> r.name("an assignment").resourceType("assignment"))
                .resource(r -> r.name("a lecture").resourceType("lecture"))
        ));

        MockHttpServletResponse response = getResource("/v1/topics/" + "urn:topic:1" + "/resources?type=" + assignment + "," + lecture);
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture".equals(r.name));
        assertAnyTrue(result, r -> "an assignment".equals(r.name));
    }

    @Test
    public void can_have_several_resource_types() throws Exception {
        ResourceType lecture = builder.resourceType("lecture", rt -> rt.name("lecture"));
        ResourceType assignment = builder.resourceType("assignment", rt -> rt.name("assignment"));

        builder.subject(s -> s.topic(t -> t
                .name("topic")
                .publicId("urn:topic:1")
                .resource(r -> r
                        .name("resource")
                        .resourceType(lecture)
                        .resourceType(assignment)
                )
        ));

        MockHttpServletResponse response = getResource("/v1/topics/" + "urn:topic:1" + "/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals(2, result[0].resourceTypes.size());
        assertAnyTrue(result[0].resourceTypes, rt -> rt.id.equals(lecture.getPublicId()));
        assertAnyTrue(result[0].resourceTypes, rt -> rt.id.equals(assignment.getPublicId()));
    }

    @Test
    public void can_have_several_resource_types_recursively() throws Exception {
        ResourceType lecture = builder.resourceType("lecture", rt -> rt.name("lecture"));
        ResourceType assignment = builder.resourceType("assignment", rt -> rt.name("assignment"));

        URI topic = builder.topic(t -> t
                .name("topic")
                .subtopic(st -> st
                        .resource(r -> r
                                .name("resource 1")
                                .resourceType(lecture)
                                .resourceType(assignment)
                        )
                )
                .resource(r -> r
                        .name("resource 2")
                        .resourceType(lecture)
                        .resourceType(assignment)
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/topics/" + topic + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertEquals(2, result[0].resourceTypes.size());
        assertEquals(2, result[1].resourceTypes.size());
        assertAnyTrue(result[0].resourceTypes, rt -> rt.id.equals(lecture.getPublicId()));
        assertAnyTrue(result[0].resourceTypes, rt -> rt.id.equals(assignment.getPublicId()));
        assertAnyTrue(result[1].resourceTypes, rt -> rt.id.equals(lecture.getPublicId()));
        assertAnyTrue(result[1].resourceTypes, rt -> rt.id.equals(assignment.getPublicId()));
    }

    @Test
    public void can_have_no_resource_type() throws Exception {
        URI topic = builder.topic(t -> t
                .name("topic")
                .subtopic(st -> st
                        .resource(r -> r
                                .name("resource 1")
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/topics/" + topic + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals(0, result[0].resourceTypes.size());
    }

    @Test
    public void can_get_resources_for_a_topic_recursively_with_translation() throws Exception {
        builder.resourceType("article", rt -> rt.name("Article").translation("nb", tr -> tr.name("Artikkel")));

        URI a = builder.topic(t -> t
                .resource(r -> r
                        .name("Introduction to calculus")
                        .translation("nb", tr -> tr.name("Introduksjon til calculus"))
                        .resourceType("article")
                )
                .subtopic(st -> st
                        .resource(r -> r
                                .name("Introduction to integration")
                                .translation("nb", tr -> tr.name("Introduksjon til integrasjon"))
                                .resourceType("article")
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/topics/" + a + "/resources?recursive=true&language=nb");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "Introduksjon til calculus".equals(r.name));
        assertAnyTrue(result, r -> "Introduksjon til integrasjon".equals(r.name));
        assertAllTrue(result, r -> r.resourceTypes.iterator().next().name.equals("Artikkel"));
    }

    @Test
    public void can_get_resources_for_a_topic_without_child_topic_resources_with_translation() throws Exception {
        builder.resourceType("article", rt -> rt.name("Article").translation("nb", tr -> tr.name("Artikkel")));

        builder.subject(s -> s
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r
                                .name("resource 1")
                                .translation("nb", tr -> tr.name("ressurs 1"))
                                .resourceType("article")
                        )
                        .resource(r -> r
                                .name("resource 2")
                                .translation("nb", tr -> tr.name("ressurs 2"))
                                .resourceType("article")
                        )
                        .subtopic(st -> st.name("subtopic").resource(r -> r.name("subtopic resource")))
                ));

        MockHttpServletResponse response = getResource("/v1/topics/" + "urn:topic:1" + "/resources?language=nb");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "ressurs 1".equals(r.name));
        assertAnyTrue(result, r -> "ressurs 2".equals(r.name));
        assertAllTrue(result, r -> r.resourceTypes.iterator().next().name.equals("Artikkel"));
    }

}




