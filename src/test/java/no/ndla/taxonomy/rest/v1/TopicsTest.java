package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.commands.TopicCommand;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.TopicDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicsTest extends RestTest {
    @Autowired
    EntityManager entityManager;

    @Autowired
    private TestSeeder testSeeder;

    @BeforeEach
    void clearAllRepos() {
        resourceRepository.deleteAllAndFlush();
        topicRepository.deleteAllAndFlush();
        subjectRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_single_topic() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .name("trigonometry")
                        .contentUri("urn:article:1")
                        .publicId("urn:topic:1")
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1");
        final var topic = testUtils.getObject(TopicDTO.class, response);

        assertEquals("trigonometry", topic.getName());
        assertEquals("urn:article:1", topic.getContentUri().toString());
        assertEquals("/subject:1/topic:1", topic.getPath());

        assertNull(topic.getMetadata());
    }

    @Test
    public void can_get_single_topic_with_metadata() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .name("trigonometry")
                        .contentUri("urn:article:1")
                        .publicId("urn:topic:1")
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1?includeMetadata=true");
        final var topic = testUtils.getObject(TopicDTO.class, response);

        assertEquals("trigonometry", topic.getName());
        assertEquals("urn:article:1", topic.getContentUri().toString());
        assertEquals("/subject:1/topic:1", topic.getPath());

        assertNotNull(topic.getMetadata());
        assertTrue(topic.getMetadata().isVisible());
        assertTrue(topic.getMetadata().getGrepCodes().size() == 1 && topic.getMetadata().getGrepCodes().contains("TOPIC1"));
    }

    @Test
    public void topic_without_subject_has_no_url() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1");
        final var topic = testUtils.getObject(TopicDTO.class, response);

        assertNull(topic.getPath());
    }

    @Test
    public void can_get_all_topics() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(t -> t.name("photo synthesis")));
        builder.subject(s -> s
                .name("Maths")
                .topic(t -> t.name("trigonometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics");
        final var topics = testUtils.getObject(TopicDTO[].class, response);
        assertEquals(2, topics.length);

        assertAnyTrue(topics, t -> "photo synthesis".equals(t.getName()));
        assertAnyTrue(topics, t -> "trigonometry".equals(t.getName()));
        assertAllTrue(topics, t -> isValidId(t.getId()));
        assertAllTrue(topics, t -> t.getPath().contains("subject") && t.getPath().contains("topic"));

        assertAllTrue(topics, t -> t.getMetadata() == null);
    }

    @Test
    public void can_get_topics_by_contentURI() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(t -> {
                    t.name("photo synthesis");
                    t.contentUri(URI.create("urn:test:1"));
                }));
        builder.subject(s -> s
                .name("Maths")
                .topic(t -> {
                    t.name("trigonometry");
                    t.contentUri(URI.create("urn:test:2"));
                }));

        {
            final var response = testUtils.getResource("/v1/topics?contentURI=urn:test:1");
            final var topics = testUtils.getObject(TopicDTO[].class, response);
            assertEquals(1, topics.length);
            assertEquals("photo synthesis", topics[0].getName());
        }

        {
            final var response = testUtils.getResource("/v1/topics?contentURI=urn:test:2");
            final var topics = testUtils.getObject(TopicDTO[].class, response);
            assertEquals(1, topics.length);
            assertEquals("trigonometry", topics[0].getName());
        }
    }


    @Test
    public void can_get_all_topics_with_metadata() throws Exception {
        builder.subject(s -> s
                .name("Basic science")
                .topic(t -> t.name("photo synthesis")));
        builder.subject(s -> s
                .name("Maths")
                .topic(t -> t.name("trigonometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/topics?includeMetadata=true");
        final var topics = testUtils.getObject(TopicDTO[].class, response);
        assertEquals(2, topics.length);

        assertAnyTrue(topics, t -> "photo synthesis".equals(t.getName()));
        assertAnyTrue(topics, t -> "trigonometry".equals(t.getName()));
        assertAllTrue(topics, t -> isValidId(t.getId()));
        assertAllTrue(topics, t -> t.getPath().contains("subject") && t.getPath().contains("topic"));

        assertAllTrue(topics, t -> t.getMetadata() != null);
        assertAllTrue(topics, t -> t.getMetadata().isVisible());
        assertAllTrue(topics, t -> t.getMetadata().getGrepCodes().size() == 1);
    }


    /**
     * This test creates a structure of subjects and topics as follows:
     * <pre>
     *   S:1
     *    \
     *     T:1
     *      \
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
        testSeeder.topicConnectionsTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:2000/connections");
        ConnectionIndexDTO[] connections = testUtils.getObject(ConnectionIndexDTO[].class, response);

        assertEquals(3, connections.length, "Correct number of connections");
        assertAllTrue(connections, c -> c.getPaths().size() > 0); //all connections have at least one path

        connectionsHaveCorrectTypes(connections);
    }

    @Test
    public void can_get_unfiltered_subtopics() throws Exception {
        testSeeder.subtopicsByTopicIdAndFiltersTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/topics");
        final var subtopics = testUtils.getObject(TopicDTO[].class, response);
        assertEquals(7, subtopics.length, "Unfiltered subtopics");

        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata() == null);
    }

    @Test
    public void can_get_unfiltered_subtopics_with_metadata() throws Exception {
        testSeeder.subtopicsByTopicIdAndFiltersTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1/topics?includeMetadata=true");
        final var subtopics = testUtils.getObject(TopicDTO[].class, response);
        assertEquals(7, subtopics.length, "Unfiltered subtopics");

        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata() != null);
        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata().isVisible());
        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata().getGrepCodes().size() == 1);
    }


    @Test
    public void can_get_filtered_subtopics() throws Exception {
        testSeeder.subtopicsByTopicIdAndFiltersTestSetup();

        {
            final var response = testUtils.getResource("/v1/topics/urn:topic:1/topics?filter=urn:filter:1");
            final var subtopics = testUtils.getObject(TopicDTO[].class, response);
            assertEquals(3, subtopics.length, "Filter 1 subtopics");
        }

        {
            final var response = testUtils.getResource("/v1/topics/urn:topic:1/topics?filter=urn:filter:2");
            final var subtopics = testUtils.getObject(TopicDTO[].class, response);
            assertEquals(4, subtopics.length, "Filter 2 subtopics");
        }
    }

    private void connectionsHaveCorrectTypes(ConnectionIndexDTO[] connections) {
        ConnectionTypeCounter connectionTypeCounter = new ConnectionTypeCounter(connections).countTypes();
        assertEquals(1, connectionTypeCounter.getParentCount());
        assertEquals(2, connectionTypeCounter.getChildCount());
    }

    @Test
    public void can_create_topic() throws Exception {
        final var createTopicCommand = new TopicCommand() {{
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
        final var createTopicCommand = new TopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "trigonometry";
        }};

        testUtils.createResource("/v1/topics", createTopicCommand);

        Topic topic = topicRepository.getByPublicId(createTopicCommand.id);
        assertEquals(createTopicCommand.name, topic.getName());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new TopicCommand() {{
            id = URI.create("urn:topic:1");
            name = "name";
        }};

        testUtils.createResource("/v1/topics", command, status().isCreated());
        testUtils.createResource("/v1/topics", command, status().isConflict());
    }

    @Test
    public void can_update_topic() throws Exception {
        URI id = builder.topic().getPublicId();

        testUtils.updateResource("/v1/topics/" + id, new TopicCommand() {{
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

        verify(metadataApiService).deleteMetadataByPublicId(parentId);
    }

    @Test
    public void can_delete_topic_with_2_resources() throws Exception {
        Topic topic = builder.topic(child -> child
                .name("MAIN TOPIC")
                .translation("nb", tr -> tr.name("HovedEmne"))
                .resource(r -> r.publicId("urn:resource:1"))
                .resource(r -> r.publicId("urn:resource:2")));

        final var topicId = topic.getPublicId();

        testUtils.deleteResource("/v1/topics/" + topicId);

        assertNull(topicRepository.findByPublicId(topicId));

        verify(metadataApiService).deleteMetadataByPublicId(topicId);
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

        verify(metadataApiService).deleteMetadataByPublicId(parentId);
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

        verify(metadataApiService).deleteMetadataByPublicId(parentId);
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

    private static class ConnectionTypeCounter {
        private final ConnectionIndexDTO[] connections;
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
                switch (connection.getType()) {
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
                        fail("Unexpected connection type :" + connection.getType());
                }
            }
            return this;
        }
    }
}
