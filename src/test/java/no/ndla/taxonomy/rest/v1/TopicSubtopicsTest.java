package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicSubtopicsTest extends RestTest {

    @Test
    public void can_add_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = builder.topic(t -> t.name("calculus")).getPublicId();
        integrationId = builder.topic(t -> t.name("integration")).getPublicId();

        URI id = getId(
                testUtils.createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }})

        );

        final var connection = topicSubtopicRepository.findByPublicId(id);
        assertTrue(connection.isPrimary());

        Topic calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getSubtopics().size());
        assertAnyTrue(calculus.getSubtopics(), t -> "integration".equals(t.getName()));
        assertNotNull(topicSubtopicRepository.getByPublicId(id));
        assertTrue(calculus.getChildrenTopicSubtopics().iterator().next().isPrimary());
    }

    @Test
    public void can_add_secondary_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = builder.topic(t -> t.name("calculus")).getPublicId();
        integrationId = builder.topic(t -> t.name("integration")).getPublicId();

        final var parentTopic2PublicId = builder.topic().getPublicId();

        // 20190819 JEP@Cerpus: Behavior change: It was possible to create a non-primary subtopic when no topic existed
        // before, but it has changed. The first connection to a topic will ignore the primary parameter and will be
        // forced to become primary (subject-topics already did this)

        URI id = getId(
                testUtils.createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                    primary = false;
                }})
        );

        Topic calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getSubtopics().size());
        assertAnyTrue(calculus.getSubtopics(), t -> "integration".equals(t.getName()));
        assertNotNull(topicSubtopicRepository.getByPublicId(id));
        assertTrue(calculus.getChildrenTopicSubtopics().iterator().next().isPrimary());

        URI connection2Id = getId(
                testUtils.createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = parentTopic2PublicId;
                    subtopicid = integrationId;
                    primary = false;
                }})
        );

        final var connection2 = topicSubtopicRepository.findFirstByPublicId(connection2Id).orElse(null);
        assertNotNull(connection2);

        assertEquals(parentTopic2PublicId, connection2.getTopic().map(Topic::getPublicId).orElse(null));
        assertEquals(integrationId, connection2.getSubtopic().map(Topic::getPublicId).orElse(null));
        assertFalse(connection2.isPrimary());
    }

    @Test
    public void cannot_add_existing_subtopic_to_topic() throws Exception {
        URI integrationId = builder.topic("integration", t -> t.name("integration")).getPublicId();
        URI calculusId = builder.topic(t -> t
                .name("calculus")
                .subtopic("integration")
        ).getPublicId();

        testUtils.createResource("/v1/topic-subtopics",
                new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }},
                status().isConflict()
        );
    }


    @Test
    public void can_delete_topic_subtopic() throws Exception {
        URI id = save(TopicSubtopic.create(newTopic(), newTopic())).getPublicId();
        testUtils.deleteResource("/v1/topic-subtopics/" + id);
        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_update_topic_subtopic() throws Exception {
        URI id = save(TopicSubtopic.create(newTopic(), newTopic(), true)).getPublicId();

        testUtils.updateResource("/v1/topic-subtopics/" + id, new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = true;
        }});

        assertTrue(topicSubtopicRepository.getByPublicId(id).isPrimary());
    }

    @Test
    public void cannot_unset_primary_topic() throws Exception {
        URI id = save(TopicSubtopic.create(newTopic(), newTopic(), true)).getPublicId();

        testUtils.updateResource("/v1/topic-subtopics/" + id, new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = false;
        }}, status().is4xxClientError());
    }

    @Test
    public void can_get_topics() throws Exception {
        URI alternatingCurrentId = builder.topic("ac", t -> t.name("alternating current")).getPublicId();
        URI electricityId = builder.topic(t -> t.name("electricity").subtopic("ac")).getPublicId();
        URI integrationId = builder.topic("integration", t -> t.name("integration")).getPublicId();
        URI calculusId = builder.topic(t -> t.name("calculus").subtopic("integration")).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/topic-subtopics");
        TopicSubtopics.TopicSubtopicIndexDocument[] topicSubtopics = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument[].class, response);

        assertEquals(2, topicSubtopics.length);
        assertAnyTrue(topicSubtopics, t -> electricityId.equals(t.topicid) && alternatingCurrentId.equals(t.subtopicid));
        assertAnyTrue(topicSubtopics, t -> calculusId.equals(t.topicid) && integrationId.equals(t.subtopicid));
        assertAllTrue(topicSubtopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topic_subtopic() throws Exception {
        URI topicid, subtopicid, id;
        Topic electricity = newTopic().name("electricity");
        Topic alternatingCurrent = newTopic().name("alternating current");
        TopicSubtopic topicSubtopic = save(TopicSubtopic.create(electricity, alternatingCurrent));

        topicid = electricity.getPublicId();
        subtopicid = alternatingCurrent.getPublicId();
        id = topicSubtopic.getPublicId();

        MockHttpServletResponse resource = testUtils.getResource("/v1/topic-subtopics/" + id);
        TopicSubtopics.TopicSubtopicIndexDocument topicSubtopicIndexDocument = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, resource);

        assertEquals(topicid, topicSubtopicIndexDocument.topicid);

        assertEquals(subtopicid, topicSubtopicIndexDocument.subtopicid);
    }

    @Test
    public void deleted_primary_parent_topic_is_replaced() throws Exception {
        Topic subtopic = builder.topic();
        Topic primary = builder.topic(t -> t.name("primary").subtopic(subtopic, true));
        builder.topic(t -> t.name("other").subtopic(subtopic));

        testUtils.deleteResource("/v1/topics/" + primary.getPublicId());

        assertEquals("other", subtopic.getPrimaryParentTopic().get().getName());
    }

    @Test
    public void setting_new_parent_makes_old_one_secondary() throws Exception {
        Topic subtopic = builder.topic(t -> t.name("a subtopic"));

        builder.topic("electricity", t -> t
                .name("electricity")
                .subtopic(subtopic, true)
        );

        Topic newPrimary = builder.topic("wiring", t -> t
                .name("Wiring")
        );

        testUtils.createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
            topicid = newPrimary.getPublicId();
            subtopicid = subtopic.getPublicId();
            primary = true;
        }});

        subtopic.getParentTopicSubtopics().forEach(topicResource -> {
            Topic topic = topicResource.getTopic().orElse(null);
            if (topic.equals(newPrimary)) assertTrue(topicResource.isPrimary());
            else assertFalse(topicResource.isPrimary());
        });

    }

    @Test
    public void setting_secondary_parent_primary_makes_old_primary_parent_secondary() throws Exception {
        Topic subtopic = builder.topic(t -> t.name("a subtopic"));

        Topic oldprimary = builder.topic("electricity", t -> t
                .name("electricity")
                .subtopic(subtopic, true)
        );

        Topic newPrimary = builder.topic("wiring", t -> t
                .name("Wiring")
        );

        testUtils.createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
            topicid = newPrimary.getPublicId();
            subtopicid = subtopic.getPublicId();
            primary = true;
        }});

        TopicSubtopic topicSubtopic = oldprimary.getChildrenTopicSubtopics().iterator().next();

        testUtils.updateResource("/v1/topic-subtopics/" + topicSubtopic.getPublicId().toString(), new TopicSubtopics.AddSubtopicToTopicCommand() {{
            primary = true;
        }});

        subtopic.getParentTopicSubtopics().forEach(topicResource -> {
            Topic topic = topicResource.getTopic().orElse(null);
            if (topic.equals(oldprimary)) assertTrue(topicResource.isPrimary());
            else assertFalse(topicResource.isPrimary());
        });
    }

    @Test
    public void subtopics_have_default_rank() throws Exception {
        builder.topic(t -> t
                .name("electricity")
                .subtopic(st -> st
                        .name("alternating currents"))
                .subtopic(st -> st
                        .name("wiring")));
        MockHttpServletResponse response = testUtils.getResource(("/v1/topic-subtopics"));
        TopicSubtopics.TopicSubtopicIndexDocument[] subtopics = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument[].class, response);

        assertAllTrue(subtopics, st -> st.rank == 0);
    }

    @Test
    public void subtopics_can_be_created_with_rank() throws Exception {
        Subject subject = builder.subject(s -> s.name("Subject").publicId("urn:subject:1"));
        Topic electricity = builder.topic(s -> s
                .name("Electricity")
                .publicId("urn:topic:1"));
        save(SubjectTopic.create(subject, electricity));
        Topic alternatingCurrents = builder.topic(t -> t
                .name("Alternating currents")
                .publicId("urn:topic:11"));
        Topic wiring = builder.topic(t -> t
                .name("Wiring")
                .publicId("urn:topic:12"));

        testUtils.createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
            topicid = electricity.getPublicId();
            subtopicid = alternatingCurrents.getPublicId();
            rank = 2;
        }});

        testUtils.createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
            topicid = electricity.getPublicId();
            subtopicid = wiring.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics?recursive=true");
        TopicSubtopics.TopicSubtopicIndexDocument[] topics = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument[].class, response);

        assertEquals(electricity.getPublicId(), topics[0].id);
        assertEquals(wiring.getPublicId(), topics[1].id);
        assertEquals(alternatingCurrents.getPublicId(), topics[2].id);
    }

    @Test
    public void can_update_subtopic_rank() throws Exception {
        URI id = save(TopicSubtopic.create(newTopic(), newTopic())).getPublicId();

        testUtils.updateResource("/v1/topic-subtopics/" + id, new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = true;
            rank = 99;
        }});

        assertEquals(99, topicSubtopicRepository.getByPublicId(id).getRank());

    }

    @Test
    public void update_subtopic_rank_modifies_other_contiguous_ranks() throws Exception {
        List<TopicSubtopic> topicSubtopics = createTenContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicSubtopics);

        //make the last object the first
        TopicSubtopic updatedConnection = topicSubtopics.get(topicSubtopics.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/topic-subtopics/" + updatedConnection.getPublicId().toString(), new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicSubtopic topicSubtopic : topicSubtopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/topic-subtopics/" + topicSubtopic.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connectionFromDb = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
            //verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_subtopic_rank_does_not_alter_noncontiguous_ranks() throws Exception {

        List<TopicSubtopic> topicSubtopics = createTenNonContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicSubtopics);

        //make the last object the first
        TopicSubtopic updatedConnection = topicSubtopics.get(topicSubtopics.size() - 1);
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource("/v1/topic-subtopics/" + updatedConnection.getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicSubtopic topicSubtopic : topicSubtopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/topic-subtopics/" + topicSubtopic.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connectionFromDb = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
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
    public void update_subtopic_rank_higher_rank_does_not_modify_existing_connections() throws Exception {
        List<TopicSubtopic> topicSubtopics = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicSubtopics);

        //set rank for last object to higher than any existing
        TopicSubtopic updatedConnection = topicSubtopics.get(topicSubtopics.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/topic-subtopics/" + topicSubtopics.get(9).getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 99;
        }});
        assertEquals(99, updatedConnection.getRank());

        //verify that the other connections are unchanged
        for (TopicSubtopic topicSubtopic : topicSubtopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/topic-subtopics/" + topicSubtopic.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connection = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    private Map<String, Integer> mapConnectionRanks(List<TopicSubtopic> topicSubtopics) {
        Map<String, Integer> mappedRanks = new HashMap<>();
        for (TopicSubtopic ts : topicSubtopics) {
            mappedRanks.put(ts.getPublicId().toString(), ts.getRank());
        }
        return mappedRanks;
    }


    private List<TopicSubtopic> createTenContiguousRankedConnections() {
        List<TopicSubtopic> connections = new ArrayList<>();
        Topic parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Topic sub = newTopic();
            TopicSubtopic topicSubtopic = TopicSubtopic.create(parent, sub);
            topicSubtopic.setRank(i);
            connections.add(topicSubtopic);
            save(topicSubtopic);
        }
        return connections;
    }

    private List<TopicSubtopic> createTenNonContiguousRankedConnections() {
        List<TopicSubtopic> connections = new ArrayList<>();
        Topic parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Topic sub = newTopic();
            TopicSubtopic topicSubtopic = TopicSubtopic.create(parent, sub);
            if (i <= 5) {
                topicSubtopic.setRank(i);
            } else {
                topicSubtopic.setRank(i * 10);
            }
            connections.add(topicSubtopic);
            save(topicSubtopic);
        }
        return connections;
    }
}
