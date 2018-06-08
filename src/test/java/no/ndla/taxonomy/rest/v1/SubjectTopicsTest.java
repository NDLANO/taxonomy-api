package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SubjectTopicsTest extends RestTest {

    @Test
    public void can_add_topic_to_subject() throws Exception {
        URI subjectId, topicId;
        subjectId = newSubject().name("physics").getPublicId();
        topicId = newTopic().name("trigonometry").getPublicId();

        URI id = getId(
                createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }})
        );

        Subject physics = subjectRepository.getByPublicId(subjectId);
        assertEquals(1, count(physics.getTopics()));
        assertAnyTrue(physics.getTopics(), t -> "trigonometry".equals(t.getName()));
        assertNotNull(subjectTopicRepository.getByPublicId(id));
    }

    @Test
    public void cannot_add_existing_topic_to_subject() throws Exception {
        Subject physics = newSubject().name("physics");
        Topic trigonometry = newTopic().name("trigonometry");
        physics.addTopic(trigonometry);

        URI subjectId = physics.getPublicId();
        URI topicId = trigonometry.getPublicId();

        createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
                    this.subjectid = subjectId;
                    this.topicid = topicId;
                }},
                status().isConflict()
        );
    }

    @Test
    public void can_delete_subject_topic() throws Exception {
        URI id = save(newSubject().addTopic(newTopic())).getPublicId();
        deleteResource("/v1/subject-topics/" + id);
        assertNull(subjectRepository.findByPublicId(id));
    }

    @Test
    public void can_update_subject_topic() throws Exception {
        URI id = save(newSubject().addTopic(newTopic())).getPublicId();

        updateResource("/v1/subject-topics/" + id, new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
        }});

        assertTrue(subjectTopicRepository.getByPublicId(id).isPrimary());
    }

    @Test
    public void cannot_unset_primary_subject() throws Exception {
        URI id = save(newSubject().addTopic(newTopic())).getPublicId();

        updateResource("/v1/subject-topics/" + id, new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = false;
        }}, status().is4xxClientError());
    }

    @Test
    public void can_get_topics() throws Exception {
        Subject physics = newSubject().name("physics");
        Topic electricity = newTopic().name("electricity");
        save(physics.addTopic(electricity));

        Subject mathematics = newSubject().name("mathematics");
        Topic trigonometry = newTopic().name("trigonometry");
        save(mathematics.addTopic(trigonometry));

        URI physicsId = physics.getPublicId();
        URI electricityId = electricity.getPublicId();
        URI mathematicsId = mathematics.getPublicId();
        URI trigonometryId = trigonometry.getPublicId();

        MockHttpServletResponse response = getResource("/v1/subject-topics");
        SubjectTopics.SubjectTopicIndexDocument[] subjectTopics = getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertEquals(2, subjectTopics.length);
        assertAnyTrue(subjectTopics, t -> physicsId.equals(t.subjectid) && electricityId.equals(t.topicid));
        assertAnyTrue(subjectTopics, t -> mathematicsId.equals(t.subjectid) && trigonometryId.equals(t.topicid));
        assertAllTrue(subjectTopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_subject_topic() throws Exception {
        Subject physics = newSubject().name("physics");
        Topic electricity = newTopic().name("electricity");
        SubjectTopic subjectTopic = save(physics.addTopic(electricity));

        URI subjectid = physics.getPublicId();
        URI topicid = electricity.getPublicId();
        URI id = subjectTopic.getPublicId();

        MockHttpServletResponse resource = getResource("/v1/subject-topics/" + id);
        SubjectTopics.SubjectTopicIndexDocument subjectTopicIndexDocument = getObject(SubjectTopics.SubjectTopicIndexDocument.class, resource);
        assertEquals(subjectid, subjectTopicIndexDocument.subjectid);
        assertEquals(topicid, subjectTopicIndexDocument.topicid);
    }

    @Test
    public void first_subject_connected_to_topic_is_primary() throws Exception {
        Subject electricity = newSubject().name("physics");
        Topic alternatingCurrent = newTopic().name("electricity");
        SubjectTopic subjectTopic = save(electricity.addTopic(alternatingCurrent));

        MockHttpServletResponse resource = getResource("/v1/subject-topics/" + subjectTopic.getPublicId());
        SubjectTopics.SubjectTopicIndexDocument subjectTopicIndexDocument = getObject(SubjectTopics.SubjectTopicIndexDocument.class, resource);
        assertTrue(subjectTopicIndexDocument.primary);
    }

    @Test
    public void topic_can_only_have_one_primary_subject() throws Exception {
        Topic topic = builder.topic("graphs", r -> r.name("graphs"));

        builder.subject("elementary maths", t -> t
                .name("elementary maths")
                .topic(topic));

        Subject newPrimary = builder.subject("graph theory", t -> t
                .name("graph theory"));

        createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
            subjectid = newPrimary.getPublicId();
            topicid = topic.getPublicId();
            primary = true;
        }});

        topic.subjects.forEach(subjectTopic -> {
            if (subjectTopic.getSubject().equals(newPrimary)) assertTrue(subjectTopic.isPrimary());
            else assertFalse(subjectTopic.isPrimary());
        });
    }

    @Test
    public void deleted_primary_subject_is_replaced() throws Exception {
        Topic topic = newTopic();
        URI primary = save(newSubject().addTopic(topic)).getPublicId();
        URI other = save(newSubject().addTopic(topic)).getPublicId();

        deleteResource("/v1/subject-topics/" + primary);

        SubjectTopic subjectTopic = topic.subjects.iterator().next();
        assertEquals(other, subjectTopic.getPublicId());
        assertTrue(subjectTopic.isPrimary());
    }

    @Test
    public void topic_has_default_rank() throws Exception {
        builder.subject(s -> s
                .name("Mathematics")
                .topic(t -> t
                        .name("Geometry")));

        MockHttpServletResponse response = getResource("/v1/subject-topics");
        SubjectTopics.SubjectTopicIndexDocument[] topics = getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertAllTrue(topics, t -> t.rank == 0);
    }

    @Test
    public void topics_can_have_same_rank() throws Exception {
        Subject mathematics = builder.subject(s -> s
                .name("Mathematics")
                .publicId("urn:subject:1"));
        Topic geometry = builder.topic(t -> t
                .name("Geometry")
                .publicId("urn:topic:1"));
        Topic statistics = builder.topic(t -> t
                .name("Statistics")
                .publicId("urn:topic:2"));
        SubjectTopic geometryMaths = save(mathematics.addTopic(geometry));
        SubjectTopic statisticsMaths = save(mathematics.addTopic(statistics));

        updateResource("/v1/subject-topics/" + geometryMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = geometryMaths.getPublicId();
            rank = 1;
        }});

        updateResource("/v1/subject-topics/" + statisticsMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = statisticsMaths.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = getResource("/v1/subject-topics");
        SubjectTopics.SubjectTopicIndexDocument[] topics = getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertAllTrue(topics, t -> t.rank == 1);
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
        SubjectTopic geometryMaths = save(mathematics.addTopic(geometry));
        SubjectTopic statisticsMaths = save(mathematics.addTopic(statistics));

        updateResource("/v1/subject-topics/" + geometryMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = geometryMaths.getPublicId();
            rank = 2;
        }});

        updateResource("/v1/subject-topics/" + statisticsMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = statisticsMaths.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = getResource("/v1/subjects/urn:subject:1/topics");
        SubjectTopics.SubjectTopicIndexDocument[] topics = getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

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
        SubjectTopic geometryMaths = save(mathematics.addTopic(geometry));
        SubjectTopic statisticsMaths = save(mathematics.addTopic(statistics));
        TopicSubtopic tst1 = save(geometry.addSubtopic(subtopic1));
        TopicSubtopic tst2 = save(geometry.addSubtopic(subtopic2));

        updateResource("/v1/subject-topics/" + geometryMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = geometryMaths.getPublicId();
            rank = 2;
        }});

        updateResource("/v1/subject-topics/" + statisticsMaths.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            id = statisticsMaths.getPublicId();
            rank = 1;
        }});

        updateResource("/v1/topic-subtopics/" + tst1.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 2;
        }});

        updateResource("/v1/topic-subtopics/" + tst2.getPublicId(), new SubjectTopics.UpdateSubjectTopicCommand() {{
            primary = true;
            rank = 1;
        }});
        MockHttpServletResponse response = getResource("/v1/subjects/urn:subject:1/topics?recursive=true");
        SubjectTopics.SubjectTopicIndexDocument[] topics = getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

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

        createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
            subjectid = mathematics.getPublicId();
            topicid = geometry.getPublicId();
            rank = 2;
        }});
        createResource("/v1/subject-topics", new SubjectTopics.AddTopicToSubjectCommand() {{
            subjectid = mathematics.getPublicId();
            topicid = statistics.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = getResource("/v1/subjects/urn:subject:1/topics");
        SubjectTopics.SubjectTopicIndexDocument[] topics = getObject(SubjectTopics.SubjectTopicIndexDocument[].class, response);

        assertEquals(statistics.getPublicId(), topics[0].id);
        assertEquals(geometry.getPublicId(), topics[1].id);
    }
}
