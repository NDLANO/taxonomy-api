package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.Arrays;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicsTest extends RestTest {

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
    public void can_get_all_subject_and_subtopic_connections() throws Exception {
        builder.subject(s -> s
                .name("Su 1")
                .publicId("urn:subject:1")
                .topic(t -> {
                    t.name("To1").publicId("urn:topic:1");
                    t.subtopic(st -> st.name("SuTo1").publicId("urn:topic:2"));
                    t.subtopic(st -> st.name("SuTo3").publicId("urn:topic:3"));
                })
        );
        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/connections");
        Topics.ConnectionIndexDocument[] connections = getObject(Topics.ConnectionIndexDocument[].class, response);
        assertEquals(3, connections.length);
        assertAllTrue(connections, c -> isValidId(c.connectionId));
        assertAllTrue(connections, c -> isValidId(c.targetId));
        assertAnyTrue(connections, c -> c.path.equals("/subject:1"));
        assertAnyTrue(connections, c -> c.path.equals("/subject:1/topic:1/topic:2"));
        assertAnyTrue(connections, c -> c.path.equals("/subject:1/topic:1/topic:3"));
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
    public void can_delete_topic_with_2_subtopics() throws Exception {
        Topic childTopic1 = builder.topic(child -> child.name("DELETE EDGE TO ME"));
        Topic childTopic2 = builder.topic(child -> child.name("DELETE EDGE TO ME ALSO"));

        URI parentId = builder.topic(parent -> parent
                .subtopic(childTopic1)
                .subtopic(childTopic2)
        ).getPublicId();

        deleteResource("/v1/topics/" + parentId);

        assertNull(topicRepository.findByPublicId(parentId));
    }

    @Test
    public void can_delete_topic_that_exists_in_2_subjects() throws Exception {
        Topic topic = builder.topic(child -> child
                .name("MAIN TOPIC")
                .translation("nb", tr -> tr.name("HovedEmne"))
                .resource(r -> r.publicId("urn:resource:1")));

        builder.subject(s -> s.name("primary").topic(topic));
        builder.subject(s -> s.name("secondary").topic(topic));

        deleteResource("/v1/topics/" + topic.getPublicId());

        assertNull(topicRepository.findByPublicId(topic.getPublicId()));
    }

    @Test
    public void can_delete_topic_that_has_2_parenttopics() throws Exception {
        Topic topic = builder.topic(child -> child
                .name("MAIN TOPIC")
                .translation("nb", tr -> tr.name("HovedEmne"))
                .resource(r -> r.publicId("urn:resource:1")));

        builder.topic(t -> t.name("primary").subtopic(topic));
        builder.topic(t -> t.name("secondary").subtopic(topic));

        deleteResource("/v1/topics/" + topic.getPublicId());

        assertNull(topicRepository.findByPublicId(topic.getPublicId()));
    }

    @Test
    public void can_delete_topic_with_2_resources() throws Exception {
        Topic topic = builder.topic(child -> child
                .name("MAIN TOPIC")
                .translation("nb", tr -> tr.name("HovedEmne"))
                .resource(r -> r.publicId("urn:resource:1"))
                .resource(r -> r.publicId("urn:resource:2")));

        deleteResource("/v1/topics/" + topic.getPublicId());

        assertNull(topicRepository.findByPublicId(topic.getPublicId()));
    }

    @Test
    public void can_delete_topic_but_subtopics_remain() throws Exception {
        Topic childTopic = builder.topic(child -> child
                .name("DELETE EDGE TO ME")
                .translation("nb", tr -> tr.name("emne"))
                .subtopic(sub -> sub.publicId("urn:topic:1"))
                .resource(r -> r.publicId("urn:resource:1")));

        URI parentId = builder.topic(parent -> parent
                .subtopic(childTopic)
        ).getPublicId();

        deleteResource("/v1/topics/" + parentId);

        assertNull(topicRepository.findByPublicId(parentId));
        assertNotNull(topicRepository.findByPublicId(childTopic.getPublicId()));
    }

    @Test
    public void can_delete_topic_but_resources_and_filter_remain() throws Exception {
        Resource resource = builder.resource("resource", r -> r
                .translation("nb", tr -> tr.name("ressurs"))
                .resourceType(rt -> rt.name("Learning path")));
        Filter filter = builder.filter(f -> f.publicId("urn:filter:1").name("Vg 1"));

        URI parentId = builder.topic(parent -> parent
                .resource(resource)
                .filter(filter, builder.relevance(rel -> rel.publicId("urn:relevance:core")))
        ).getPublicId();

        deleteResource("/v1/topics/" + parentId);

        assertNull(topicRepository.findByPublicId(parentId));
        assertNotNull(resourceRepository.findByPublicId(resource.getPublicId()));
        assertNotNull(filterRepository.findByPublicId(filter.getPublicId()));
    }

    @Test
    public void can_get_resource_connection_id() throws Exception {
        Topic topic = builder.topic(t -> t
                .publicId("urn:topic:1")
                .resource()
        );
        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(first(topic.resources).getPublicId(), result[0].connectionId);
    }

    @Test
    public void can_get_resource_connection_id_recursively() throws Exception {
        builder.topic("topic", t -> t
                .publicId("urn:topic:1")
                .resource(r -> r
                        .name("a")
                        .publicId("urn:resource:1"))
                .subtopic("subtopic", st -> st
                        .publicId("urn:topic:2")
                        .resource(r -> r.name("b")
                                .publicId("urn:resource:2")))
        );

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(first(builder.topic("topic").resources).getPublicId(), result[0].connectionId);
        assertEquals(first(builder.topic("subtopic").resources).getPublicId(), result[1].connectionId);
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

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:a/resources?recursive=true");
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
                                        .resource("aaa", r -> r.publicId("urn:resource:aaa"))
                                )
                                .subtopic(st2 -> st2
                                        .publicId("urn:topic:aab")
                                        .resource(r -> r.publicId("urn:resource:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:a/resources?recursive=true");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(result, r -> "/subject:1/topic:a/resource:a".equals(r.path));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/resource:aa".equals(r.path));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/topic:aaa/resource:aaa".equals(r.path));
        assertAnyTrue(result, r -> "/subject:1/topic:a/topic:aa/topic:aab/resource:aab".equals(r.path));
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
            MockHttpServletResponse response = getResource("/v1/topics/urn:topic:" + i + "/resources");
            Topics.ResourceIndexDocument[] resources = getObject(Topics.ResourceIndexDocument[].class, response);

            assertEquals(1, resources.length);
            assertEquals("/subject:" + i + "/topic:" + i + "/resource:1", resources[0].path);
        }
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

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/resources");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "resource 1".equals(r.name));
        assertAnyTrue(result, r -> "resource 2".equals(r.name));
    }

    @Test
    public void can_get_primary_and_secondary_subtopics() throws Exception {
        Topic externalTopic = builder.topic("secondary topic", t -> t
                .name("secondary topic")
                .publicId("urn:topic:b"));

        URI parentTopicId = URI.create("urn:topic:a");
        builder.subject("subject", s -> s
                .name("subject")
                .publicId("urn:subject:1")
                .topic("parent", t -> t
                        .name("parent topic")
                        .publicId(parentTopicId.toString())
                        .subtopic("child aa", child -> child
                                .name("child topic aa")
                                .publicId("urn:topic:aa")
                                .subtopic("ignored grandchild", grandchild -> grandchild
                                        .name("ignored grandchild topic")
                                        .publicId("urn:topic:aaa")
                                )
                        )
                        .subtopic("child ab", child -> child
                                .name("child topic ab")
                                .publicId("urn:topic:ab")
                        )
                        .subtopic(externalTopic, false)
                )
        );

        MockHttpServletResponse response = getResource("/v1/topics/" + parentTopicId + "/topics");
        Topics.SubTopicIndexDocument[] topics = getObject(Topics.SubTopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertEquals("child topic aa", topics[1].name);
        assertEquals("urn:topic:aa", topics[1].id.toString());
        assertTrue(topics[1].isPrimary);
        assertEquals("child topic ab", topics[2].name);
        assertEquals("urn:topic:ab", topics[2].id.toString());
        assertTrue(topics[2].isPrimary);
        assertEquals("secondary topic", topics[0].name);
        assertEquals("urn:topic:b", topics[0].id.toString());
        assertFalse(topics[0].isPrimary);
    }

    @Test
    public void can_get_primary_and_secondary_ressources() throws Exception {
        Resource externalResource = builder.resource(r -> r
                .name("external resource")
                .publicId("urn:resource:ext"));
        Topic externalTopic = builder.topic("secondary topic", t -> t
                .name("secondary topic")
                .publicId("urn:topic:b")
                .resource(externalResource));

        URI primaryTopicId = URI.create("urn:topic:pri");
        builder.subject("subject", s -> s
                .name("subject")
                .publicId("urn:subject:1")
                .topic("parent", t -> t
                        .name("parent topic")
                        .publicId(primaryTopicId.toString())
                        .resource("primary resource", child -> child
                                .name("primary resource")
                                .publicId("urn:resource:pri")
                        )
                        .resource(externalResource, false)
                )
        );

        MockHttpServletResponse response = getResource("/v1/topics/" + primaryTopicId + "/resources");
        Topics.ResourceIndexDocument[] resources = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);
        assertEquals("primary resource", resources[1].name);
        assertEquals("urn:resource:pri", resources[1].id.toString());
        assertTrue(resources[1].isPrimary);
        assertEquals("external resource", resources[0].name);
        assertEquals("urn:resource:ext", resources[0].id.toString());
        assertFalse(resources[0].isPrimary);
    }
}
