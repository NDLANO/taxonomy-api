package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.commands.CreateTopicCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateTopicCommand;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.topics.TopicIndexDocument;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.SubTopicIndexDTO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicsTest extends RestTest {
    @Autowired
    EntityManager entityManager;

    @Test
    public void can_get_single_topic() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(true, t -> t
                        .name("trigonometry")
                        .contentUri("urn:article:1")
                        .publicId("urn:topic:1")
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/" + "urn:topic:1");
        TopicIndexDocument topic = testUtils.getObject(TopicIndexDocument.class, response);

        assertEquals("trigonometry", topic.name);
        assertEquals("urn:article:1", topic.contentUri.toString());
        assertEquals("/subject:1/topic:1", topic.path);
    }

    @Test
    public void primary_url_is_returned_when_getting_single_topic() throws Exception {
        {
            builder.subject(s -> s
                    .publicId("urn:subject:1")
                    .topic("topic", false, t -> t.publicId("urn:topic:1"))
            );

            builder.subject(s -> s
                    .publicId("urn:subject:2")
                    .topic("topic", true)
            );

            var topic = builder.topic("topic");

            entityManager.merge(topic);
        }


        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1");
        TopicIndexDocument topic = testUtils.getObject(TopicIndexDocument.class, response);

        assertEquals("/subject:2/topic:1", topic.path);
    }

    @Test
    public void topic_without_subject_has_no_url() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1");
        TopicIndexDocument topic = testUtils.getObject(TopicIndexDocument.class, response);

        assertNull(topic.path);
    }

    @Test
    public void can_get_all_topics() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(true, t -> t.name("photo synthesis")));
        builder.subject(s -> s
                .name("Maths")
                .topic(true, t -> t.name("trigonometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics");
        TopicIndexDocument[] topics = testUtils.getObject(TopicIndexDocument[].class, response);
        assertEquals(2, topics.length);

        assertAnyTrue(topics, s -> "photo synthesis".equals(s.name));
        assertAnyTrue(topics, s -> "trigonometry".equals(s.name));
        assertAllTrue(topics, s -> isValidId(s.id));
        assertAllTrue(topics, t -> t.path.contains("subject") && t.path.contains("topic"));
    }


    /**
     * This test creates a structure of subjects and topics as follows:
     * <pre>
     *   S:1         S:2
     *    \         /
     *     T:1    /
     *      \   /
     *       T:2
     *      /  \
     *    T:3   T:4
     * </pre>
     * <p>
     * S:1 = urn:subject:1000
     * S:2 = urn:subject:2000
     * T:1 = urn:topic:1000
     * T:2 = urn:topic:2000
     * T:3 = urn:topic:3000
     * T:4 = urn:topic:4000
     * <p>
     * The test examines the T:2 node and verifies that it reports the correct parent-subject, parent-topic and
     * subtopic connections. As shown in the figure above, it should have 1 parent-subject (S:2), 1 parent-topic (T:1),
     * and 2 subtopics (T:3 and T:4).
     */
    @Test
    public void can_get_all_connections() throws Exception {

        executeSqlScript("classpath:topic_connections_test_setup.sql", false);
        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:2000/connections");
        ConnectionIndexDTO[] connections = testUtils.getObject(ConnectionIndexDTO[].class, response);

        assertEquals("Correct number of connections", 4, connections.length);
        assertAllTrue(connections, c -> c.paths.size() > 0); //all connections have at least one path

        connectionsHaveCorrectTypes(connections);
        primaryConnectionsAreReportedCorrectly(connections);
    }

    @Test
    public void can_get_unfiltered_subtopics() throws Exception {
        executeSqlScript("classpath:subtopics_by_topic_id_and_filters_test_setup.sql", false);
        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/topics");
        TopicIndexDocument[] subtopics = testUtils.getObject(TopicIndexDocument[].class, response);
        assertEquals("Unfiltered subtopics", 7, subtopics.length);
    }

    @Test
    public void can_get_filtered_subtopics() throws Exception {
        executeSqlScript("classpath:subtopics_by_topic_id_and_filters_test_setup.sql", false);
        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/topics?filter=urn:filter:1");
        TopicIndexDocument[] subtopics = testUtils.getObject(TopicIndexDocument[].class, response);
        assertEquals("Filter 1 subtopics", 3, subtopics.length);

        response = testUtils.getResource("/v1/topics/urn:topic:1/topics?filter=urn:filter:2");
        subtopics = testUtils.getObject(TopicIndexDocument[].class, response);
        assertEquals("Filter 2 subtopics", 4, subtopics.length);
    }

    private void primaryConnectionsAreReportedCorrectly(ConnectionIndexDTO[] connections) {
        Map<String, ConnectionIndexDTO> connectionsByName = new HashMap<>();
        Arrays.stream(connections).forEach(c -> connectionsByName.put(c.targetId.toString(), c));
        assertFalse(connectionsByName.containsKey("urn:subject:1000"));
        assertFalse(connectionsByName.get("urn:subject:2000").isPrimary);
        assertFalse(connectionsByName.get("urn:topic:4000").isPrimary);
        assertTrue(connectionsByName.get("urn:topic:1000").isPrimary);
        assertTrue(connectionsByName.get("urn:topic:3000").isPrimary);
    }

    private void connectionsHaveCorrectTypes(ConnectionIndexDTO[] connections) {
        ConnectionTypeCounter connectionTypeCounter = new ConnectionTypeCounter(connections).countTypes();
        assertEquals(1, connectionTypeCounter.getSubjectCount());
        assertEquals(1, connectionTypeCounter.getParentCount());
        assertEquals(2, connectionTypeCounter.getChildCount());
    }

    @Test
    public void can_create_topic() throws Exception {
        CreateTopicCommand createTopicCommand = new CreateTopicCommand() {{
            name = "trigonometry";
            contentUri = URI.create("urn:article:1");
        }};

        MockHttpServletResponse response = testUtils.createResource("/v1/topics", createTopicCommand);
        URI id = getId(response);

        Topic topic = topicRepository.getByPublicId(id);
        assertEquals(createTopicCommand.name, topic.getName());
        assertEquals(createTopicCommand.contentUri, topic.getContentUri());
    }

    @Test
    public void can_create_topic_with_id() throws Exception {
        CreateTopicCommand createTopicCommand = new CreateTopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "trigonometry";
        }};

        testUtils.createResource("/v1/topics", createTopicCommand);

        Topic topic = topicRepository.getByPublicId(createTopicCommand.id);
        assertEquals(createTopicCommand.name, topic.getName());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        CreateTopicCommand command = new CreateTopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "name";
        }};

        testUtils.createResource("/v1/topics", command, status().isCreated());
        testUtils.createResource("/v1/topics", command, status().isConflict());
    }

    @Test
    public void can_update_topic() throws Exception {
        URI id = builder.topic().getPublicId();

        testUtils.updateResource("/v1/topics/" + id, new UpdateTopicCommand() {{
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

        testUtils.deleteResource("/v1/topics/" + parentId);

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

        testUtils.deleteResource("/v1/topics/" + topic.getPublicId());

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

        testUtils.deleteResource("/v1/topics/" + topic.getPublicId());

        assertNull(topicRepository.findByPublicId(topic.getPublicId()));
    }

    @Test
    public void can_delete_topic_with_2_resources() throws Exception {
        Topic topic = builder.topic(child -> child
                .name("MAIN TOPIC")
                .translation("nb", tr -> tr.name("HovedEmne"))
                .resource(r -> r.publicId("urn:resource:1"))
                .resource(r -> r.publicId("urn:resource:2")));

        testUtils.deleteResource("/v1/topics/" + topic.getPublicId());

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

        testUtils.deleteResource("/v1/topics/" + parentId);

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

        testUtils.deleteResource("/v1/topics/" + parentId);

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
        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(first(topic.getTopicResources()).getPublicId(), result[0].connectionId);
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

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?recursive=true");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(first(builder.topic("topic").getTopicResources()).getPublicId(), result[0].connectionId);
        assertEquals(first(builder.topic("subtopic").getTopicResources()).getPublicId(), result[1].connectionId);
    }

    @Test
    public void can_get_resources_for_a_topic_recursively() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .name("subject a")
                .topic(true, t -> t
                        .publicId("urn:topic:a")
                        .name("a")
                        .resource(true, r -> r
                                .publicId("urn:resource:1")
                                .name("resource a").contentUri("urn:article:a"))
                        .subtopic(true, st -> st
                                .publicId("urn:topic:a:1")
                                .name("aa")
                                .resource(true, r -> r.name("resource aa").contentUri("urn:article:aa"))
                                .subtopic(true, st2 -> st2
                                        .publicId("urn:topic:a:1:1")
                                        .name("aaa")
                                        .resource(true, r -> r.name("resource aaa").contentUri("urn:article:aaa"))
                                )
                                .subtopic(true, st2 -> st2
                                        .publicId("urn:topic:a:1:2")
                                        .name("aab")
                                        .resource(true, r -> r.name("resource aab").contentUri("urn:article:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(4, result.length);
        assertAnyTrue(result, r -> "resource a".equals(r.name) && "urn:article:a".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aa".equals(r.name) && "urn:article:aa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aaa".equals(r.name) && "urn:article:aaa".equals(r.contentUri.toString()));
        assertAnyTrue(result, r -> "resource aab".equals(r.name) && "urn:article:aab".equals(r.contentUri.toString()));
        assertAllTrue(result, r -> !r.path.isEmpty());
    }

    @Test
    public void resources_by_topic_id_recursively_are_ordered_by_rank_in_parent() throws Exception {
        executeSqlScript("classpath:resources_by_topic_id_test_setup.sql", false);
        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:5/resources?recursive=true");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);
        assertEquals(6, result.length);
        assertEquals("urn:resource:3", result[0].id.toString());
        assertEquals("urn:resource:5", result[1].id.toString());
        assertEquals("urn:resource:4", result[2].id.toString());
        assertEquals("urn:resource:6", result[3].id.toString());
        assertEquals("urn:resource:7", result[4].id.toString());
        assertEquals("urn:resource:8", result[5].id.toString());

    }


    @Test
    public void can_get_urls_for_resources_for_a_topic_recursively() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1")
                .topic(true, t -> t
                        .publicId("urn:topic:a")
                        .resource(true, r -> r.publicId("urn:resource:a"))
                        .subtopic(true, st -> st
                                .publicId("urn:topic:aa")
                                .resource(true, r -> r.publicId("urn:resource:aa"))
                                .subtopic(true, st2 -> st2
                                        .publicId("urn:topic:aaa")
                                        .resource("aaa", true, r -> r.publicId("urn:resource:aaa"))
                                )
                                .subtopic(true, st2 -> st2
                                        .publicId("urn:topic:aab")
                                        .resource(true, r -> r.publicId("urn:resource:aab"))
                                )
                        )
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:a/resources?recursive=true");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

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

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

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
                .topic("parent", true, t -> t
                        .name("parent topic")
                        .publicId(parentTopicId.toString())
                        .subtopic("child aa", true, child -> child
                                .name("child topic aa")
                                .publicId("urn:topic:aa")
                                .subtopic("ignored grandchild", true, grandchild -> grandchild
                                        .name("ignored grandchild topic")
                                        .publicId("urn:topic:aaa")
                                )
                        )
                        .subtopic("child ab", true, child -> child
                                .name("child topic ab")
                                .publicId("urn:topic:ab")
                        )
                        .subtopic(externalTopic, false)
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/" + parentTopicId + "/topics");
        SubTopicIndexDTO[] topics = testUtils.getObject(SubTopicIndexDTO[].class, response);

        // Result is not sorted, order is not important
        var topicList = Arrays.stream(topics).sorted(Comparator.comparing(topic -> topic.id)).collect(Collectors.toList());

        assertEquals(3, topicList.size());

        var topic1 = topicList.get(0);
        var topic2 = topicList.get(1);
        var topic3 = topicList.get(2);

        assertEquals("child topic aa", topic1.name);
        assertEquals("urn:topic:aa", topic1.id.toString());
        assertTrue(topic1.isPrimary);
        assertEquals("child topic ab", topic2.name);
        assertEquals("urn:topic:ab", topic2.id.toString());
        assertTrue(topic2.isPrimary);
        assertEquals("secondary topic", topic3.name);
        assertEquals("urn:topic:b", topic3.id.toString());
        assertFalse(topic3.isPrimary);
    }

    @Test
    @Transactional
    public void can_get_filters_for_topic() throws Exception {
        final var topic1 = builder.topic(builder -> builder.publicId("urn:topic:1"));
        final var topic2 = builder.topic(builder -> builder.publicId("urn:topic:2"));
        builder.topic(builder -> builder.publicId("urn:topic:3"));

        final var filter1 = builder.filter(builder -> builder.publicId("urn:filter:1"));
        final var filter2 = builder.filter(builder -> builder.publicId("urn:filter:2"));
        final var filter3 = builder.filter(builder -> builder.publicId("urn:filter:3"));

        final var relevance1 = builder.relevance();
        final var relevance2 = builder.relevance();
        final var relevance3 = builder.relevance();

        TopicFilter.create(topic1, filter1, relevance1);
        TopicFilter.create(topic2, filter1, relevance1);
        TopicFilter.create(topic2, filter2, relevance2);

        final var resource1 = builder.resource();
        resource1.addFilter(filter3, relevance3);
        TopicResource.create(topic1, resource1);

        {
            final var returnedFilters = Arrays.asList(testUtils.getObject(Filters.FilterIndexDocument[].class, testUtils.getResource("/v1/topics/urn:topic:1/filters")));

            assertEquals(1, returnedFilters.size());
            assertTrue(returnedFilters
                    .stream()
                    .map(Filters.FilterIndexDocument::getId)
                    .collect(Collectors.toList()).contains(new URI("urn:filter:1")));
        }

        {
            final var returnedFilters = Arrays.asList(testUtils.getObject(Filters.FilterIndexDocument[].class, testUtils.getResource("/v1/topics/urn:topic:2/filters")));

            assertEquals(2, returnedFilters.size());
            assertTrue(returnedFilters
                    .stream()
                    .map(Filters.FilterIndexDocument::getId)
                    .collect(Collectors.toList()).containsAll(Set.of(new URI("urn:filter:1"), new URI("urn:filter:2"))));
        }

        {
            final var returnedFilters = Arrays.asList(testUtils.getObject(Filters.FilterIndexDocument[].class, testUtils.getResource("/v1/topics/urn:topic:3/filters")));

            assertEquals(0, returnedFilters.size());
        }
    }

    @Test
    public void resources_can_be_filtered_by_relevance() throws Exception {
        executeSqlScript("classpath:resource_with_filters_and_relevances_test_setup.sql", false);

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?relevance=urn:relevance:core");
        ResourceIndexDocument[] resources = testUtils.getObject(ResourceIndexDocument[].class, response);
        assertEquals(10, resources.length);

        MockHttpServletResponse response2 = testUtils.getResource("/v1/topics/urn:topic:1/resources?relevance=urn:relevance:supplementary");
        ResourceIndexDocument[] resources2 = testUtils.getObject(ResourceIndexDocument[].class, response2);
        assertEquals(5, resources2.length);

    }

    @Test
    public void resources_can_be_filtered_by_filters() throws Exception {
        executeSqlScript("classpath:resource_with_filters_and_relevances_test_setup.sql", false);

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/resources?filter=urn:filter:1");
        ResourceIndexDocument[] resources = testUtils.getObject(ResourceIndexDocument[].class, response);
        assertEquals(5, resources.length);

        MockHttpServletResponse response2 = testUtils.getResource("/v1/topics/urn:topic:1/resources?filter=urn:filter:1,urn:filter:2");
        ResourceIndexDocument[] resources2 = testUtils.getObject(ResourceIndexDocument[].class, response2);
        assertEquals(10, resources2.length);
    }

    @Test
    public void resources_can_be_filtered_by_subject_filters() throws Exception {
        final var subject1 = builder.subject(builder -> builder.publicId("urn:subject:1"));
        final var subject2 = builder.subject(builder -> builder.publicId("urn:subject:2"));
        final var subject3 = builder.subject(builder -> builder.publicId("urn:subject:3"));

        final var relevance1 = builder.relevance(builder -> builder.publicId("urn:relevance:1"));
        final var filter1 = builder.filter(builder -> builder.publicId("urn:filter:1"));
        final var filter2 = builder.filter(builder -> builder.publicId("urn:filter:2"));

        final var topic1 = builder.topic(builder -> builder.publicId("urn:topic:1"));
        final var topic2 = builder.topic(builder -> builder.publicId("urn:topic:2"));
        final var topic3 = builder.topic(builder -> builder.publicId("urn:topic:3"));

        SubjectTopic.create(subject1, topic1);
        SubjectTopic.create(subject1, topic2);
        SubjectTopic.create(subject1, topic3);

        final var resource1 = builder.resource(builder -> builder.publicId("urn:resource:1"));
        final var resource2 = builder.resource(builder -> builder.publicId("urn:resource:2"));
        final var resource3 = builder.resource(builder -> builder.publicId("urn:resource:3"));

        resource1.addFilter(filter1, relevance1);
        resource2.addFilter(filter2, relevance1);

        subject1.addFilter(filter1);
        subject2.addFilter(filter2);

        TopicResource.create(topic1, resource1);
        TopicResource.create(topic1, resource2);
        TopicResource.create(topic1, resource3);

        TopicResource.create(topic2, resource1);
        TopicResource.create(topic2, resource2);
        TopicResource.create(topic2, resource3);

        TopicResource.create(topic3, resource1);
        TopicResource.create(topic3, resource2);
        TopicResource.create(topic3, resource3);

        {
            final var resources = Arrays.asList(testUtils.getObject(ResourceIndexDocument[].class, testUtils.getResource("/v1/topics/urn:topic:1/resources")));
            assertEquals(3, resources.size());
            assertTrue(
                    resources.stream()
                            .map(ResourceIndexDocument::getId)
                            .collect(Collectors.toSet())
                            .containsAll(Set.of(new URI("urn:resource:1"), new URI("urn:resource:2"), new URI("urn:resource:3")))
            );
        }

        {
            final var resources = Arrays.asList(testUtils.getObject(ResourceIndexDocument[].class, testUtils.getResource("/v1/topics/urn:topic:1/resources?subject=urn:subject:1")));
            assertEquals(1, resources.size());
            assertTrue(
                    resources.stream()
                            .map(ResourceIndexDocument::getId)
                            .collect(Collectors.toSet())
                            .contains(new URI("urn:resource:1"))
            );
        }

        {
            final var resources = Arrays.asList(testUtils.getObject(ResourceIndexDocument[].class, testUtils.getResource("/v1/topics/urn:topic:1/resources?subject=urn:subject:2")));
            assertTrue(
                    resources.stream()
                            .map(ResourceIndexDocument::getId)
                            .collect(Collectors.toSet())
                            .contains(new URI("urn:resource:2"))
            );
        }

        // subject:3 has no filters assigned, resulting in no filters, and then returning all topics
        {
            final var resources = Arrays.asList(testUtils.getObject(ResourceIndexDocument[].class, testUtils.getResource("/v1/topics/urn:topic:1/resources?subject=urn:subject:3")));
            assertEquals(3, resources.size());
            assertTrue(
                    resources.stream()
                            .map(ResourceIndexDocument::getId)
                            .collect(Collectors.toSet())
                            .containsAll(Set.of(new URI("urn:resource:1"), new URI("urn:resource:2"), new URI("urn:resource:3"))
                            )
            );
        }
    }

    private static class ConnectionTypeCounter {
        private ConnectionIndexDTO[] connections;
        private int subjectCount;
        private int parentCount;
        private int childCount;

        ConnectionTypeCounter(ConnectionIndexDTO[] connections) {
            this.connections = connections;
        }

        int getSubjectCount() {
            return subjectCount;
        }

        int getParentCount() {
            return parentCount;
        }

        int getChildCount() {
            return childCount;
        }

        ConnectionTypeCounter countTypes() {
            subjectCount = 0;
            parentCount = 0;
            childCount = 0;
            for (ConnectionIndexDTO connection : connections) {
                switch (connection.type) {
                    case "parent-subject":
                        subjectCount++;
                        break;
                    case "parent-topic":
                        parentCount++;
                        break;
                    case "subtopic":
                        childCount++;
                        break;
                    default:
                        fail("Unexpected connection type :" + connection.type);
                }
            }
            return this;
        }
    }
}
