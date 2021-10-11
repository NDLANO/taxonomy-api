/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicSubtopicsTest extends RestTest {

    @Test
    public void can_add_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = builder.node(t -> t.nodeType(NodeType.TOPIC).name("calculus")).getPublicId();
        integrationId = builder.node(t -> t.nodeType(NodeType.TOPIC).name("integration")).getPublicId();

        URI id = getId(
                testUtils.createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }})

        );

        final var connection = nodeConnectionRepository.findByPublicId(id);

        Node calculus = nodeRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getChildNodes().size());
        assertAnyTrue(calculus.getChildNodes(), t -> "integration".equals(t.getName()));
        assertNotNull(nodeConnectionRepository.getByPublicId(id));
    }

    @Test
    public void cannot_add_existing_subtopic_to_topic() throws Exception {
        URI integrationId = builder.node("integration", t -> t.nodeType(NodeType.TOPIC).name("integration")).getPublicId();
        URI calculusId = builder.node(t -> t
                .nodeType(NodeType.TOPIC)
                .name("calculus")
                .child("integration")
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
        URI id = save(NodeConnection.create(newTopic(), newTopic())).getPublicId();
        testUtils.deleteResource("/v1/topic-subtopics/" + id);
        assertNull(nodeRepository.findByPublicId(id));
    }

    @Test
    public void can_get_topics() throws Exception {
        URI alternatingCurrentId = builder.node("ac", t -> t.nodeType(NodeType.TOPIC).name("alternating current")).getPublicId();
        URI electricityId = builder.node(t -> t.nodeType(NodeType.TOPIC).name("electricity").child("ac", c -> c.nodeType(NodeType.TOPIC))).getPublicId();
        URI integrationId = builder.node("integration", t -> t.nodeType(NodeType.TOPIC).name("integration")).getPublicId();
        URI calculusId = builder.node(t -> t.nodeType(NodeType.TOPIC).name("calculus").child("integration", c -> c.nodeType(NodeType.TOPIC))).getPublicId();

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
        Node electricity = newTopic().name("electricity");
        Node alternatingCurrent = newTopic().name("alternating current");
        NodeConnection topicSubtopic = save(NodeConnection.create(electricity, alternatingCurrent));

        topicid = electricity.getPublicId();
        subtopicid = alternatingCurrent.getPublicId();
        id = topicSubtopic.getPublicId();

        MockHttpServletResponse resource = testUtils.getResource("/v1/topic-subtopics/" + id);
        TopicSubtopics.TopicSubtopicIndexDocument topicSubtopicIndexDocument = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, resource);

        assertEquals(topicid, topicSubtopicIndexDocument.topicid);

        assertEquals(subtopicid, topicSubtopicIndexDocument.subtopicid);
    }

    @Test
    public void subtopics_have_default_rank() throws Exception {
        builder.node(t -> t
                .nodeType(NodeType.TOPIC)
                .name("electricity")
                .child(st -> st
                        .nodeType(NodeType.TOPIC)
                        .name("alternating currents"))
                .child(st -> st
                        .nodeType(NodeType.TOPIC)
                        .name("wiring")));
        MockHttpServletResponse response = testUtils.getResource(("/v1/topic-subtopics"));
        TopicSubtopics.TopicSubtopicIndexDocument[] subtopics = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument[].class, response);

        assertAllTrue(subtopics, st -> st.rank == 0);
    }

    @Test
    public void subtopics_can_be_created_with_rank() throws Exception {
        Node subject = builder.node(s -> s.nodeType(NodeType.SUBJECT).isContext(true).name("Subject").publicId("urn:subject:1"));
        Node electricity = builder.node(s -> s
                .nodeType(NodeType.TOPIC)
                .name("Electricity")
                .publicId("urn:topic:1"));
        save(NodeConnection.create(subject, electricity));
        Node alternatingCurrents = builder.node(t -> t
                .nodeType(NodeType.TOPIC)
                .name("Alternating currents")
                .publicId("urn:topic:11"));
        Node wiring = builder.node(t -> t
                .nodeType(NodeType.TOPIC)
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
        URI id = save(NodeConnection.create(newTopic(), newTopic())).getPublicId();

        testUtils.updateResource("/v1/topic-subtopics/" + id, new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = true;
            rank = 99;
        }});

        assertEquals(99, nodeConnectionRepository.getByPublicId(id).getRank());

    }

    @Test
    public void update_subtopic_rank_modifies_other_contiguous_ranks() throws Exception {
        List<NodeConnection> topicSubtopics = createTenContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicSubtopics);

        //make the last object the first
        NodeConnection updatedConnection = topicSubtopics.get(topicSubtopics.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/topic-subtopics/" + updatedConnection.getPublicId().toString(), new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (NodeConnection topicSubtopic : topicSubtopics) {
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

        List<NodeConnection> topicSubtopics = createTenNonContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicSubtopics);

        //make the last object the first
        NodeConnection updatedConnection = topicSubtopics.get(topicSubtopics.size() - 1);
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource("/v1/topic-subtopics/" + updatedConnection.getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (NodeConnection topicSubtopic : topicSubtopics) {
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
        List<NodeConnection> topicSubtopics = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicSubtopics);

        //set rank for last object to higher than any existing
        NodeConnection updatedConnection = topicSubtopics.get(topicSubtopics.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/topic-subtopics/" + topicSubtopics.get(9).getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 99;
        }});
        assertEquals(99, updatedConnection.getRank());

        //verify that the other connections are unchanged
        for (NodeConnection topicSubtopic : topicSubtopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/topic-subtopics/" + topicSubtopic.getPublicId().toString());
            TopicSubtopics.TopicSubtopicIndexDocument connection = testUtils.getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    private Map<String, Integer> mapConnectionRanks(List<NodeConnection> topicSubtopics) {
        Map<String, Integer> mappedRanks = new HashMap<>();
        for (NodeConnection ts : topicSubtopics) {
            mappedRanks.put(ts.getPublicId().toString(), ts.getRank());
        }
        return mappedRanks;
    }


    private List<NodeConnection> createTenContiguousRankedConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Node sub = newTopic();
            NodeConnection topicSubtopic = NodeConnection.create(parent, sub);
            topicSubtopic.setRank(i);
            connections.add(topicSubtopic);
            save(topicSubtopic);
        }
        return connections;
    }

    private List<NodeConnection> createTenNonContiguousRankedConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Node sub = newTopic();
            NodeConnection topicSubtopic = NodeConnection.create(parent, sub);
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
