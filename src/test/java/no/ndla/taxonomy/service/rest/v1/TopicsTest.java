package no.ndla.taxonomy.service.rest.v1;


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
        URI trigonometry = builder.topic(s -> s
                .name("trigonometry")
                .contentUri("urn:article:1")
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/topics/" + trigonometry);
        Topics.TopicIndexDocument topic = getObject(Topics.TopicIndexDocument.class, response);

        assertEquals("trigonometry", topic.name);
        assertEquals("urn:article:1", topic.contentUri.toString());
    }

    @Test
    public void can_get_all_topics() throws Exception {
        builder.topic(t -> t.name("photo synthesis"));
        builder.topic(t -> t.name("trigonometry"));

        MockHttpServletResponse response = getResource("/v1/topics");
        Topics.TopicIndexDocument[] topics = getObject(Topics.TopicIndexDocument[].class, response);
        assertEquals(2, topics.length);

        assertAnyTrue(topics, s -> "photo synthesis".equals(s.name));
        assertAnyTrue(topics, s -> "trigonometry".equals(s.name));
        assertAllTrue(topics, s -> isValidId(s.id));
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
        URI id = builder.topic().getPublicId();
        deleteResource("/v1/topics/" + id);
        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_get_resources_for_a_topic_recursively() throws Exception {
        URI a = builder.topic(t -> t
                .name("a")
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
        ).getPublicId();

        flush();

        MockHttpServletResponse response = getResource("/v1/topics/" + a + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.name) && "urn:article:a".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.name) && "urn:article:aa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aaa".equals(r.name) && "urn:article:aaa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aab".equals(r.name) && "urn:article:aab".equals(r.contentUri.toString()));
    }

    @Test
    public void can_get_resources_for_a_topic_without_child_topic_resources() throws Exception {
        URI a = builder.topic(t -> t
                .name("a")
                .subtopic(st -> st.name("subtopic").resource(r -> r.name("subtopic resource")))
                .resource(r -> r.name("resource 1"))
                .resource(r -> r.name("resource 2"))
        ).getPublicId();

        flush();

        MockHttpServletResponse response = getResource("/v1/topics/" + a + "/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "resource 1".equals(r.name));
        assertAnyTrue(result, r -> "resource 2".equals(r.name));
    }

    @Test
    public void can_get_resources_for_a_topic_filtered_on_resource_type() throws Exception {
        URI assignment = builder.resourceType("assignment").getPublicId();
        URI lecture = builder.resourceType("lecture").getPublicId();

        Topic a = builder.topic(t -> t
                .name("a")
                .subtopic(sub -> sub.name("subtopic").resource(r -> r.name("a lecture in a subtopic").resourceType("lecture")))
                .resource(r -> r.name("an assignment").resourceType("assignment"))
                .resource(r -> r.name("a lecture").resourceType("lecture"))
        );

        flush();

        MockHttpServletResponse response = getResource("/v1/topics/" + a.getPublicId() + "/resources?type=" + assignment + "," + lecture);
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture".equals(r.name));
        assertAnyTrue(result, r -> "an assignment".equals(r.name));
    }

    @Test
    public void can_have_several_resource_types() throws Exception {
        URI topic = builder.topic(t -> t
                .name("topic")
                .resource(r -> r
                        .name("resource")
                        .resourceType(rt -> rt.name("lecture"))
                        .resourceType(rt -> rt.name("assignment"))
                )
        ).getPublicId();
        flush();

        MockHttpServletResponse response = getResource("/v1/topics/" + topic + "/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals(2, result[0].resourceTypes.size());
    }

    @Test
    public void can_have_several_resource_types_recursively() throws Exception {
        URI topic = builder.topic(t -> t
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
        ).getPublicId();
        flush();

        MockHttpServletResponse response = getResource("/v1/topics/" + topic + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertEquals(2, result[0].resourceTypes.size());
        assertEquals(2, result[1].resourceTypes.size());
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
        flush();

        MockHttpServletResponse response = getResource("/v1/topics/" + topic + "/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertEquals(0, result[0].resourceTypes.size());
    }
}




