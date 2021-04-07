package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.service.dtos.SubNodeIndexDTO;
import org.junit.jupiter.api.BeforeEach;
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

public class NodeSubnodeTest extends RestTest {

    @BeforeEach
    void clearAllRepos() {
        topicRepository.deleteAllAndFlush();
    }

    @Test
    public void can_add_subject_to_node() throws Exception {
        URI nodeId = URI.create("urn:programarea:1");
        URI subjectId;
        newNode(newNodeType(URI.create("urn:nodetype:programarea"), "Program area"), nodeId, "Test program area");
        subjectId = newSubject().name("physics").getPublicId();

        URI id = getId(
                testUtils.createResource("/v1/node-subnodes", new NodeSubnodes.AddSubnodeToNodeCommand() {{
                    this.nodeid = nodeId;
                    this.subnodeid = subjectId;
                }})
        );

        final var connection = topicSubtopicRepository.findByPublicId(id);

        Topic area = topicRepository.getByPublicId(nodeId);
        assertEquals(1, area.getSubtopics().size());
        assertAnyTrue(area.getSubtopics(), t -> "physics".equals(t.getName()));
        assertNotNull(topicSubtopicRepository.getByPublicId(id));
    }

    @Test
    public void can_add_topic_to_subject() throws Exception {
        URI subjectId, topicId;
        subjectId = newSubject().name("physics").getPublicId();
        topicId = newTopic().name("trigonometry").getPublicId();

        URI id = getId(
                testUtils.createResource("/v1/node-subnodes", new NodeSubnodes.AddSubnodeToNodeCommand() {{
                    this.nodeid = subjectId;
                    this.subnodeid = topicId;
                }})
        );

        final var connection = topicSubtopicRepository.findByPublicId(id);

        Topic physics = topicRepository.getByPublicId(subjectId);
        assertEquals(1, physics.getSubtopics().size());
        assertAnyTrue(physics.getSubtopics(), t -> "trigonometry".equals(t.getName()));
        assertNotNull(topicSubtopicRepository.getByPublicId(id));
    }

    @Test
    public void can_add_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = builder.topic(t -> t.name("calculus")).getPublicId();
        integrationId = builder.topic(t -> t.name("integration")).getPublicId();

        URI id = getId(
                testUtils.createResource("/v1/node-subnodes", new NodeSubnodes.AddSubnodeToNodeCommand() {{
                    nodeid = calculusId;
                    subnodeid = integrationId;
                }})

        );

        final var connection = topicSubtopicRepository.findByPublicId(id);

        Topic calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getSubtopics().size());
        assertAnyTrue(calculus.getSubtopics(), t -> "integration".equals(t.getName()));
        assertNotNull(topicSubtopicRepository.getByPublicId(id));
    }

    @Test
    public void cannot_add_existing_subject_to_node() throws Exception {
        Topic node = newNode(newNodeType(URI.create("urn:nodetype:programarea"), "Program area"), URI.create("urn:programarea:1"), "Test program area");
        Topic physics = newSubject().name("physics");
        TopicSubtopic.create(node, physics);

        URI nodeId = node.getPublicId();
        URI subjectId = physics.getPublicId();

        testUtils.createResource("/v1/node-subnodes", new NodeSubnodes.AddSubnodeToNodeCommand() {{
                    this.nodeid = nodeId;
                    this.subnodeid = subjectId;
                }},
                status().isConflict()
        );
    }

    @Test
    public void cannot_add_existing_topic_to_subject() throws Exception {
        Topic physics = newSubject().name("physics");
        Topic trigonometry = newTopic().name("trigonometry");
        TopicSubtopic.create(physics, trigonometry);

        URI subjectId = physics.getPublicId();
        URI topicId = trigonometry.getPublicId();

        testUtils.createResource("/v1/node-subnodes", new NodeSubnodes.AddSubnodeToNodeCommand() {{
                    this.nodeid = subjectId;
                    this.subnodeid = topicId;
                }},
                status().isConflict()
        );
    }

    @Test
    public void cannot_add_existing_subtopic_to_topic() throws Exception {
        URI integrationId = builder.topic("integration", t -> t.name("integration")).getPublicId();
        URI calculusId = builder.topic(t -> t
                .name("calculus")
                .subtopic("integration")
        ).getPublicId();

        testUtils.createResource("/v1/node-subnodes",
                new NodeSubnodes.AddSubnodeToNodeCommand() {{
                    nodeid = calculusId;
                    subnodeid = integrationId;
                }},
                status().isConflict()
        );
    }


    @Test
    public void can_delete_node_subject() throws Exception {
        Topic node = newNode(newNodeType(URI.create("urn:nodetype:programarea"), "Program area"), URI.create("urn:programarea:1"), "Test program area");
        URI id = save(TopicSubtopic.create(node, newSubject())).getPublicId();
        testUtils.deleteResource("/v1/node-subnodes/" + id);
        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_delete_subject_topic() throws Exception {
        URI id = save(TopicSubtopic.create(newSubject(), newTopic())).getPublicId();
        testUtils.deleteResource("/v1/node-subnodes/" + id);
        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_delete_topic_subtopic() throws Exception {
        URI id = save(TopicSubtopic.create(newTopic(), newTopic())).getPublicId();
        testUtils.deleteResource("/v1/node-subnodes/" + id);
        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_get_topics() throws Exception {
        URI alternatingCurrentId = builder.topic("ac", t -> t.name("alternating current")).getPublicId();
        URI electricityId = builder.topic(t -> t.name("electricity").subtopic("ac")).getPublicId();
        URI integrationId = builder.topic("integration", t -> t.name("integration")).getPublicId();
        URI calculusId = builder.topic(t -> t.name("calculus").subtopic("integration")).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/node-subnodes");
        NodeSubnodes.NodeSubnodeIndexDocument[] nodeSubnodes = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument[].class, response);

        assertEquals(2, nodeSubnodes.length);
        assertAnyTrue(nodeSubnodes, t -> electricityId.equals(t.nodeid) && alternatingCurrentId.equals(t.subnodeid));
        assertAnyTrue(nodeSubnodes, t -> calculusId.equals(t.nodeid) && integrationId.equals(t.subnodeid));
        assertAllTrue(nodeSubnodes, t -> isValidId(t.id));
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

        MockHttpServletResponse resource = testUtils.getResource("/v1/node-subnodes/" + id);
        NodeSubnodes.NodeSubnodeIndexDocument topicSubtopicIndexDocument = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, resource);

        assertEquals(topicid, topicSubtopicIndexDocument.nodeid);

        assertEquals(subtopicid, topicSubtopicIndexDocument.subnodeid);
    }

    @Test
    public void subtopics_have_default_rank() throws Exception {
        builder.topic(t -> t
                .name("electricity")
                .subtopic(st -> st
                        .name("alternating currents"))
                .subtopic(st -> st
                        .name("wiring")));
        MockHttpServletResponse response = testUtils.getResource(("/v1/node-subnodes"));
        NodeSubnodes.NodeSubnodeIndexDocument[] subtopics = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument[].class, response);

        assertAllTrue(subtopics, st -> st.rank == 0);
    }

    @Test
    public void subtopics_can_be_created_with_rank() throws Exception {
        Topic subject = builder.subject(s -> s.name("Subject").publicId("urn:subject:1"));
        Topic electricity = builder.topic(s -> s
                .name("Electricity")
                .publicId("urn:topic:1"));
        save(TopicSubtopic.create(subject, electricity));
        Topic alternatingCurrents = builder.topic(t -> t
                .name("Alternating currents")
                .publicId("urn:topic:11"));
        Topic wiring = builder.topic(t -> t
                .name("Wiring")
                .publicId("urn:topic:12"));

        testUtils.createResource("/v1/node-subnodes", new NodeSubnodes.AddSubnodeToNodeCommand() {{
            nodeid = electricity.getPublicId();
            subnodeid = alternatingCurrents.getPublicId();
            rank = 2;
        }});

        testUtils.createResource("/v1/node-subnodes", new NodeSubnodes.AddSubnodeToNodeCommand() {{
            nodeid = electricity.getPublicId();
            subnodeid = wiring.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics?recursive=true");
        NodeSubnodes.NodeSubnodeIndexDocument[] topics = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument[].class, response);

        assertEquals(electricity.getPublicId(), topics[0].id);
        assertEquals(wiring.getPublicId(), topics[1].id);
        assertEquals(alternatingCurrents.getPublicId(), topics[2].id);
    }

    @Test
    public void can_update_subject_rank() throws Exception {
        URI id = save(TopicSubtopic.create(newSubject(), newTopic())).getPublicId();

        MockHttpServletResponse responseBefore = testUtils.getResource("/v1/node-subnodes/" + id.toString());
        NodeSubnodes.NodeSubnodeIndexDocument connection = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, responseBefore);
        assertEquals(0, connection.rank);

        testUtils.updateResource("/v1/node-subnodes/" + id, new NodeSubnodes.UpdateNodeSubnodeCommand() {{
            rank = 12;
        }});

        MockHttpServletResponse responseAfter = testUtils.getResource("/v1/node-subnodes/" + id.toString());
        NodeSubnodes.NodeSubnodeIndexDocument connectionAfter = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, responseAfter);

        assertEquals(12, connectionAfter.rank);
    }

    @Test
    public void can_update_subtopic_rank() throws Exception {
        URI id = save(TopicSubtopic.create(newTopic(), newTopic())).getPublicId();

        testUtils.updateResource("/v1/node-subnodes/" + id, new NodeSubnodes.UpdateNodeSubnodeCommand() {{
            rank = 99;
        }});

        assertEquals(99, topicSubtopicRepository.getByPublicId(id).getRank());

    }

    @Test
    public void update_subject_rank_modifies_other_contiguous_ranks() throws Exception {
        List<TopicSubtopic> subjectTopics = createTenContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(subjectTopics);

        //make the last object the first
        TopicSubtopic updatedConnection = subjectTopics.get(subjectTopics.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-subnodes/" + updatedConnection.getPublicId().toString(), new NodeSubnodes.UpdateNodeSubnodeCommand() {{
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicSubtopic subjectTopic : subjectTopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/node-subnodes/" + subjectTopic.getPublicId().toString());
            NodeSubnodes.NodeSubnodeIndexDocument connectionFromDb = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, response);
            //verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_subtopic_rank_modifies_other_contiguous_ranks() throws Exception {
        List<TopicSubtopic> nodeSubnodes = createTenContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeSubnodes);

        //make the last object the first
        TopicSubtopic updatedConnection = nodeSubnodes.get(nodeSubnodes.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-subnodes/" + updatedConnection.getPublicId().toString(), new NodeSubnodes.UpdateNodeSubnodeCommand() {{
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicSubtopic topicSubtopic : nodeSubnodes) {
            MockHttpServletResponse response = testUtils.getResource("/v1/node-subnodes/" + topicSubtopic.getPublicId().toString());
            NodeSubnodes.NodeSubnodeIndexDocument connectionFromDb = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, response);
            //verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_subject_rank_modifies_other_noncontiguous_ranks() throws Exception {

        List<TopicSubtopic> subjectTopics = createTenNonContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(subjectTopics);

        //make the last object the first
        TopicSubtopic updatedConnection = subjectTopics.get(subjectTopics.size() - 1);
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-subnodes/" + updatedConnection.getPublicId().toString(), new NodeSubnodes.UpdateNodeSubnodeCommand() {{
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicSubtopic subjectTopic : subjectTopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/node-subnodes/" + subjectTopic.getPublicId().toString());
            NodeSubnodes.NodeSubnodeIndexDocument connectionFromDb = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, response);
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
    public void update_subtopic_rank_does_not_alter_noncontiguous_ranks() throws Exception {

        List<TopicSubtopic> nodeSubnodes = createTenNonContiguousRankedConnections(); //creates ranks 1, 2, 3, 4, 5, 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeSubnodes);

        //make the last object the first
        TopicSubtopic updatedConnection = nodeSubnodes.get(nodeSubnodes.size() - 1);
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-subnodes/" + updatedConnection.getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        assertEquals(1, updatedConnection.getRank());

        //verify that the other connections have been updated
        for (TopicSubtopic topicSubtopic : nodeSubnodes) {
            MockHttpServletResponse response = testUtils.getResource("/v1/node-subnodes/" + topicSubtopic.getPublicId().toString());
            NodeSubnodes.NodeSubnodeIndexDocument connectionFromDb = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, response);
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
        List<TopicSubtopic> subjectTopics = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(subjectTopics);

        //set rank for last object to higher than any existing
        TopicSubtopic updatedConnection = subjectTopics.get(subjectTopics.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-subnodes/" + subjectTopics.get(9).getPublicId().toString(), new NodeSubnodes.UpdateNodeSubnodeCommand() {{
            rank = 99;
        }});
        assertEquals(99, updatedConnection.getRank());

        //verify that the other connections are unchanged
        for (TopicSubtopic subjectTopic : subjectTopics) {
            MockHttpServletResponse response = testUtils.getResource("/v1/node-subnodes/" + subjectTopic.getPublicId().toString());
            NodeSubnodes.NodeSubnodeIndexDocument connection = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    @Test
    public void update_subtopic_rank_higher_rank_does_not_modify_existing_connections() throws Exception {
        List<TopicSubtopic> nodeSubnodes = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeSubnodes);

        //set rank for last object to higher than any existing
        TopicSubtopic updatedConnection = nodeSubnodes.get(nodeSubnodes.size() - 1);
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource("/v1/node-subnodes/" + nodeSubnodes.get(9).getPublicId().toString(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 99;
        }});
        assertEquals(99, updatedConnection.getRank());

        //verify that the other connections are unchanged
        for (TopicSubtopic topicSubtopic : nodeSubnodes) {
            MockHttpServletResponse response = testUtils.getResource("/v1/node-subnodes/" + topicSubtopic.getPublicId().toString());
            NodeSubnodes.NodeSubnodeIndexDocument connection = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    @Test
    public void can_get_subject_topic() throws Exception {
        Topic physics = newSubject().name("physics");
        Topic electricity = newTopic().name("electricity");
        TopicSubtopic subjectTopic = save(TopicSubtopic.create(physics, electricity));

        URI subjectid = physics.getPublicId();
        URI topicid = electricity.getPublicId();
        URI id = subjectTopic.getPublicId();

        MockHttpServletResponse resource = testUtils.getResource("/v1/node-subnodes/" + id);
        NodeSubnodes.NodeSubnodeIndexDocument subjectTopicIndexDocument = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument.class, resource);
        assertEquals(subjectid, subjectTopicIndexDocument.nodeid);
        assertEquals(topicid, subjectTopicIndexDocument.subnodeid);
    }

    @Test
    public void topic_has_default_rank() throws Exception {
        builder.subject(s -> s
                .name("Mathematics")
                .topic(t -> t
                        .name("Geometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/node-subnodes");
        NodeSubnodes.NodeSubnodeIndexDocument[] topics = testUtils.getObject(NodeSubnodes.NodeSubnodeIndexDocument[].class, response);

        assertAllTrue(topics, t -> t.rank == 0);
    }

    @Test
    public void can_change_sorting_order_for_topics() throws Exception {
        Topic mathematics = builder.subject(s -> s
                .name("Mathematics")
                .publicId("urn:subject:1"));
        Topic geometry = builder.topic(t -> t
                .name("Geometry")
                .publicId("urn:topic:1"));
        Topic statistics = builder.topic(t -> t
                .name("Statistics")
                .publicId("urn:topic:2"));
        TopicSubtopic geometryMaths = save(TopicSubtopic.create(mathematics, geometry));
        TopicSubtopic statisticsMaths = save(TopicSubtopic.create(mathematics, statistics));

        testUtils.updateResource("/v1/node-subnodes/" + geometryMaths.getPublicId(), new NodeSubnodes.UpdateNodeSubnodeCommand() {{
            id = geometryMaths.getPublicId();
            rank = 2;
        }});

        testUtils.updateResource("/v1/node-subnodes/" + statisticsMaths.getPublicId(), new NodeSubnodes.UpdateNodeSubnodeCommand() {{
            id = statisticsMaths.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:subject:1/nodes");
        SubNodeIndexDTO[] topics = testUtils.getObject(SubNodeIndexDTO[].class, response);

        assertEquals(statistics.getPublicId(), topics[0].getId());
        assertEquals(geometry.getPublicId(), topics[1].getId());
    }

    @Test
    public void can_change_sorting_order_for_subtopics() throws Exception {
        Topic mathematics = builder.subject(s -> s
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
        TopicSubtopic geometryMaths = save(TopicSubtopic.create(mathematics, geometry));
        TopicSubtopic statisticsMaths = save(TopicSubtopic.create(mathematics, statistics));
        TopicSubtopic tst1 = save(TopicSubtopic.create(geometry, subtopic1));
        TopicSubtopic tst2 = save(TopicSubtopic.create(geometry, subtopic2));

        testUtils.updateResource("/v1/node-subnodes/" + geometryMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = geometryMaths.getPublicId();
            rank = 2;
        }});

        testUtils.updateResource("/v1/node-subnodes/" + statisticsMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = statisticsMaths.getPublicId();
            rank = 1;
        }});

        testUtils.updateResource("/v1/node-subnodes/" + tst1.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 2;
        }});

        testUtils.updateResource("/v1/node-subnodes/" + tst2.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        {
            MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:subject:1/nodes");
            SubNodeIndexDTO[] topics = testUtils.getObject(SubNodeIndexDTO[].class, response);

            assertEquals(statistics.getPublicId(), topics[0].getId());
            assertEquals(geometry.getPublicId(), topics[1].getId());
        }
        {
            MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes");
            SubNodeIndexDTO[] topics = testUtils.getObject(SubNodeIndexDTO[].class, response);

            assertEquals(subtopic2.getPublicId(), topics[0].getId());
            assertEquals(subtopic1.getPublicId(), topics[1].getId());
        }
    }

    @Test
    public void can_create_topic_with_rank() throws Exception {
        Topic mathematics = builder.subject(s -> s
                .name("Mathematics")
                .publicId("urn:subject:1"));
        Topic geometry = builder.topic(t -> t
                .name("Geometry")
                .publicId("urn:topic:1"));
        Topic statistics = builder.topic(t -> t
                .name("Statistics")
                .publicId("urn:topic:2"));

        testUtils.createResource("/v1/node-subnodes", new NodeSubnodes.AddSubnodeToNodeCommand() {{
            nodeid = mathematics.getPublicId();
            subnodeid = geometry.getPublicId();
            rank = 2;
        }});
        testUtils.createResource("/v1/node-subnodes", new NodeSubnodes.AddSubnodeToNodeCommand() {{
            nodeid = mathematics.getPublicId();
            subnodeid = statistics.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:subject:1/nodes");
        SubNodeIndexDTO[] topics = testUtils.getObject(SubNodeIndexDTO[].class, response);

        assertEquals(statistics.getPublicId(), topics[0].getId());
        assertEquals(geometry.getPublicId(), topics[1].getId());
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
