package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.service.RankableConnectionUpdater;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SubjectTopicsTest extends RestTest {

    @Test
    public void can_add_topic_to_subject() throws Exception {
        URI subjectId, topicId;
        subjectId = newSubject().name("physics").getPublicId();
        topicId = newTopic().name("trigonometry").getPublicId();

        URI id = getId(
                testUtils.createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }})
        );

        final var connection = subjectTopicRepository.findByPublicId(id);

        Subject physics = subjectRepository.getByPublicId(subjectId);
        assertEquals(1, physics.getTopics().size());
        assertAnyTrue(physics.getTopics(), t -> "trigonometry".equals(t.getName()));
        assertNotNull(subjectTopicRepository.getByPublicId(id));
    }

    @Test
    public void cannot_add_existing_topic_to_subject() throws Exception {
        Subject physics = newSubject().name("physics");
        Topic trigonometry = newTopic().name("trigonometry");
        SubjectTopic.create(physics, trigonometry);

        URI subjectId = physics.getPublicId();
        URI topicId = trigonometry.getPublicId();

        testUtils.createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }},
                status().isConflict()
        );
    }

    @Test
    public void can_delete_subject_topic() throws Exception {
        URI id = save(SubjectTopic.create(newSubject(), newTopic())).getPublicId();
        testUtils.deleteResource("/v1/subject-topics/" + id);
        assertNull(subjectRepository.findByPublicId(id));
    }

    @Test
    public void can_update_subject_rank() throws Exception {
        URI id = save(SubjectTopic.create(newSubject(), newTopic())).getPublicId();

        MockHttpServletResponse responseBefore = testUtils.getResource("/v1/subject-topics/" + id.toString());
        SubjectTopics.SubjectTopicIndexDocument connection = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument.class, responseBefore);
        assertEquals(0, connection.rank);

        testUtils.updateResource("/v1/subject-topics/" + id, new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 12;
        }});

        MockHttpServletResponse responseAfter = testUtils.getResource("/v1/subject-topics/" + id.toString());
        SubjectTopics.SubjectTopicIndexDocument connectionAfter = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument.class, responseAfter);

        assertEquals(12, connectionAfter.rank);
    }

    @Test
    public void update_subject_rank_modifies_other_contiguous_ranks() throws Exception {
        List<SubjectTopic> subjectTopics = createTenContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(subjectTopics);

        //make the last object the first
        SubjectTopic updatedConnection = subjectTopics.get(subjectTopics.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/subject-topics/" + updatedConnection.getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (SubjectTopic subjectTopic : subjectTopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/subject-topics/" + subjectTopic.getPublicId().toString());
            SubjectTopics.SubjectTopicIndexDocument connectionFromDb = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument.class, response);
            //verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_subject_rank_modifies_other_noncontiguous_ranks() throws Exception {

        List<SubjectTopic> subjectTopics = createTenNonContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(subjectTopics);

        //make the last object the first
        SubjectTopic updatedConnection = subjectTopics.get(subjectTopics.size() - 1);
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource("/v1/subject-topics/" + updatedConnection.getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (SubjectTopic subjectTopic : subjectTopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/subject-topics/" + subjectTopic.getPublicId().toString());
            SubjectTopics.SubjectTopicIndexDocument connectionFromDb = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument.class, response);
            //verify that only the contiguous connections are updated
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                if (oldRank <= 5) {
                    assertEquals(oldRank + 1, connectionFromDb.rank);
                } else {
                    assertEquals(oldRank, connectionFromDb.rank);
                }
            }
        }
    }


    @Test
    public void update_subject_rank_higher_rank_does_not_modify_existing_connections() throws Exception {
        List<SubjectTopic> subjectTopics = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(subjectTopics);

        //set rank for last object to higher than any existing
        SubjectTopic updatedConnection = subjectTopics.get(subjectTopics.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/subject-topics/" + subjectTopics.get(9).getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 99;
        }});
        assertEquals(99, updatedConnection.getRank());

        //verify that the other connections are unchanged
        for (SubjectTopic subjectTopic : subjectTopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/subject-topics/" + subjectTopic.getPublicId().toString());
            SubjectTopics.SubjectTopicIndexDocument connection = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    @Test
    public void update_subject_rank_no_existing_connections_returns_single_connection() {
        Subject s = new Subject();
        Topic t = new Topic();

        SubjectTopic st = SubjectTopic.create(s, t);
        List<SubjectTopic> rankedList = RankableConnectionUpdater.rank(new ArrayList<>(), st, 99);
        assertEquals(1, rankedList.size());

    }

    @Test
    public void can_get_topics() throws Exception {
        Subject physics = newSubject().name("physics");
        Topic electricity = newTopic().name("electricity");
        save(SubjectTopic.create(physics, electricity));

        Subject mathematics = newSubject().name("mathematics");
        Topic trigonometry = newTopic().name("trigonometry");
        save(SubjectTopic.create(mathematics, trigonometry));

        URI physicsId = physics.getPublicId();
        URI electricityId = electricity.getPublicId();
        URI mathematicsId = mathematics.getPublicId();
        URI trigonometryId = trigonometry.getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subject-topics");
        SubjectTopics.SubjectTopicIndexDocument[] subjectTopics = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertEquals(2, subjectTopics.length);
        assertAnyTrue(subjectTopics, t -> physicsId.equals(t.subjectid) && electricityId.equals(t.topicid));
        assertAnyTrue(subjectTopics, t -> mathematicsId.equals(t.subjectid) && trigonometryId.equals(t.topicid));
        assertAllTrue(subjectTopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_subject_topic() throws Exception {
        Subject physics = newSubject().name("physics");
        Topic electricity = newTopic().name("electricity");
        SubjectTopic subjectTopic = save(SubjectTopic.create(physics, electricity));

        URI subjectid = physics.getPublicId();
        URI topicid = electricity.getPublicId();
        URI id = subjectTopic.getPublicId();

        MockHttpServletResponse resource = testUtils.getResource("/v1/subject-topics/" + id);
        SubjectTopics.SubjectTopicIndexDocument subjectTopicIndexDocument = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument.class, resource);
        assertEquals(subjectid, subjectTopicIndexDocument.subjectid);
        assertEquals(topicid, subjectTopicIndexDocument.topicid);
    }

    @Test
    public void topic_has_default_rank() throws Exception {
        builder.subject(s -> s
                .name("Mathematics")
                .topic(t -> t
                        .name("Geometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/subject-topics");
        SubjectTopics.SubjectTopicIndexDocument[] topics = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertAllTrue(topics, t -> t.rank == 0);
    }

    @Test
    public void can_change_sorting_order_for_topics() throws Exception {
        Subject mathematics = builder.subject(s -> s
                .name("Mathematics")
                .publicId("urn:subject:1"));
        Topic geometry = builder.topic(t -> t
                .name("Geometry")
                .publicId("urn:topic:1"));
        Topic statistics = builder.topic(t -> t
                .name("Statistics")
                .publicId("urn:topic:2"));
        SubjectTopic geometryMaths = save(SubjectTopic.create(mathematics, geometry));
        SubjectTopic statisticsMaths = save(SubjectTopic.create(mathematics, statistics));

        testUtils.updateResource("/v1/subject-topics/" + geometryMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = geometryMaths.getPublicId();
            rank = 2;
        }});

        testUtils.updateResource("/v1/subject-topics/" + statisticsMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = statisticsMaths.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/topics");
        SubjectTopics.SubjectTopicIndexDocument[] topics = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertEquals(statistics.getPublicId(), topics[0].id);
        assertEquals(geometry.getPublicId(), topics[1].id);
    }

    @Test
    public void can_change_sorting_order_for_subtopics() throws Exception {
        Subject mathematics = builder.subject(s -> s
                .name("Mathematics")
                .publicId("urn:subject:1"));
        Topic geometry = builder.topic(t -> t
                .name("Geometry")
                .publicId("urn:topic:1"));
        Topic statistics = builder.topic(t -> t
                .name("Statistics")
                .publicId("urn:topic:2"));
        Topic subtopic1 = builder.topic(t -> t
                .name("Subtopic 1")
                .publicId("urn:topic:aa"));
        Topic subtopic2 = builder.topic(t -> t
                .name("Subtopic 2")
                .publicId("urn:topic:ab"));
        SubjectTopic geometryMaths = save(SubjectTopic.create(mathematics, geometry));
        SubjectTopic statisticsMaths = save(SubjectTopic.create(mathematics, statistics));
        TopicSubtopic tst1 = save(TopicSubtopic.create(geometry, subtopic1));
        TopicSubtopic tst2 = save(TopicSubtopic.create(geometry, subtopic2));

        testUtils.updateResource("/v1/subject-topics/" + geometryMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = geometryMaths.getPublicId();
            rank = 2;
        }});

        testUtils.updateResource("/v1/subject-topics/" + statisticsMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = statisticsMaths.getPublicId();
            rank = 1;
        }});

        testUtils.updateResource("/v1/topic-subtopics/" + tst1.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 2;
        }});

        testUtils.updateResource("/v1/topic-subtopics/" + tst2.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/topics?recursive=true");
        SubjectTopics.SubjectTopicIndexDocument[] topics = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertEquals(statistics.getPublicId(), topics[0].id);
        assertEquals(geometry.getPublicId(), topics[1].id);
        assertEquals(subtopic2.getPublicId(), topics[2].id);
        assertEquals(subtopic1.getPublicId(), topics[3].id);
    }

    @Test
    public void can_create_topic_with_rank() throws Exception {
        Subject mathematics = builder.subject(s -> s
                .name("Mathematics")
                .publicId("urn:subject:1"));
        Topic geometry = builder.topic(t -> t
                .name("Geometry")
                .publicId("urn:topic:1"));
        Topic statistics = builder.topic(t -> t
                .name("Statistics")
                .publicId("urn:topic:2"));

        testUtils.createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
            subjectid = mathematics.getPublicId();
            topicid = geometry.getPublicId();
            rank = 2;
        }});
        testUtils.createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
            subjectid = mathematics.getPublicId();
            topicid = statistics.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/topics");
        SubjectTopics.SubjectTopicIndexDocument[] topics = testUtils.getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertEquals(statistics.getPublicId(), topics[0].id);
        assertEquals(geometry.getPublicId(), topics[1].id);
    }

    private Map<String, Integer> mapConnectionRanks(List<SubjectTopic> subjectTopics) {
        Map<String, Integer> mappedRanks = new HashMap<>();
        for (SubjectTopic st : subjectTopics) {
            mappedRanks.put(st.getPublicId().toString(), st.getRank());
        }
        return mappedRanks;
    }


    private List<SubjectTopic> createTenContiguousRankedConnections() {
        List<SubjectTopic> connections = new ArrayList<>();
        Subject s = newSubject();
        for (int i = 1; i < 11; i++) {
            Topic t = newTopic();
            SubjectTopic subjectTopic = SubjectTopic.create(s, t);
            subjectTopic.setRank(i);
            connections.add(subjectTopic);
            save(subjectTopic);
        }
        return connections;
    }

    private List<SubjectTopic> createTenNonContiguousRankedConnections() {
        List<SubjectTopic> connections = new ArrayList<>();
        Subject s = newSubject();
        for (int i = 1; i < 11; i++) {
            Topic t = newTopic();
            SubjectTopic subjectTopic = SubjectTopic.create(s, t);
            if (i <= 5) {
                subjectTopic.setRank(i);
            } else {
                subjectTopic.setRank(i * 10);
            }
            connections.add(subjectTopic);
            save(subjectTopic);
        }
        return connections;
    }
}
