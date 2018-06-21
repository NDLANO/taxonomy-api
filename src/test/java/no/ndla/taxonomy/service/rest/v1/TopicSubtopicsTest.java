package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicSubtopicsTest extends RestTest {

    @Test
    public void can_add_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = builder.topic(t -> t.name("calculus")).getPublicId();
        integrationId = builder.topic(t -> t.name("integration")).getPublicId();

        URI id = getId(
                createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }})

        );

        Topic calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, count(calculus.getSubtopics()));
        assertAnyTrue(calculus.getSubtopics(), t -> "integration".equals(t.getName()));
        assertNotNull(topicSubtopicRepository.getByPublicId(id));
        assertTrue(calculus.subtopics.iterator().next().isPrimary());
    }

    @Test
    public void can_add_secondary_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = builder.topic(t -> t.name("calculus")).getPublicId();
        integrationId = builder.topic(t -> t.name("integration")).getPublicId();

        URI id = getId(
                createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                    primary = false;
                }})
        );

        Topic calculus = topicRepository.getByPublicId(calculusId);
        assertEquals(1, count(calculus.getSubtopics()));
        assertAnyTrue(calculus.getSubtopics(), t -> "integration".equals(t.getName()));
        assertNotNull(topicSubtopicRepository.getByPublicId(id));
        assertFalse(calculus.subtopics.iterator().next().isPrimary());
    }

    @Test
    public void cannot_add_existing_subtopic_to_topic() throws Exception {
        URI integrationId = builder.topic("integration", t -> t.name("integration")).getPublicId();
        URI calculusId = builder.topic(t -> t
                .name("calculus")
                .subtopic("integration")
        ).getPublicId();

        createResource("/v1/topic-subtopics",
                new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = calculusId;
                    subtopicid = integrationId;
                }},
                status().isConflict()
        );
    }


    @Test
    public void can_delete_topic_subtopic() throws Exception {
        URI id = save(newTopic().addSubtopic(newTopic())).getPublicId();
        deleteResource("/v1/topic-subtopics/" + id);
        assertNull(topicRepository.findByPublicId(id));
    }

    @Test
    public void can_update_topic_subtopic() throws Exception {
        URI id = save(newTopic().addSubtopic(newTopic())).getPublicId();

        updateResource("/v1/topic-subtopics/" + id, new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = true;
        }});

        assertTrue(topicSubtopicRepository.getByPublicId(id).isPrimary());
    }

    @Test
    public void cannot_unset_primary_topic() throws Exception {
        URI id = save(newTopic().addSubtopic(newTopic())).getPublicId();

        updateResource("/v1/topic-subtopics/" + id, new TopicSubtopics.UpdateTopicSubtopicCommand() {{
            primary = false;
        }}, status().is4xxClientError());
    }

    @Test
    public void can_get_topics() throws Exception {
        URI alternatingCurrentId = builder.topic("ac", t -> t.name("alternating current")).getPublicId();
        URI electricityId = builder.topic(t -> t.name("electricity").subtopic("ac")).getPublicId();
        URI integrationId = builder.topic("integration", t -> t.name("integration")).getPublicId();
        URI calculusId = builder.topic(t -> t.name("calculus").subtopic("integration")).getPublicId();

        MockHttpServletResponse response = getResource("/v1/topic-subtopics");
        TopicSubtopics.TopicSubtopicIndexDocument[] topicSubtopics = getObject(TopicSubtopics.TopicSubtopicIndexDocument[].class, response);

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
        TopicSubtopic topicSubtopic = save(electricity.addSubtopic(alternatingCurrent));

        topicid = electricity.getPublicId();
        subtopicid = alternatingCurrent.getPublicId();
        id = topicSubtopic.getPublicId();

        MockHttpServletResponse resource = getResource("/v1/topic-subtopics/" + id);
        TopicSubtopics.TopicSubtopicIndexDocument topicSubtopicIndexDocument = getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, resource);

        assertEquals(topicid, topicSubtopicIndexDocument.topicid);

        assertEquals(subtopicid, topicSubtopicIndexDocument.subtopicid);
    }

    @Test
    public void first_topic_connected_to_subtopic_is_primary() throws Exception {
        Topic electricity = newTopic().name("electricity");
        Topic alternatingCurrent = newTopic().name("How alternating current works");
        TopicSubtopic topicSubtopic = save(electricity.addSubtopic(alternatingCurrent));

        MockHttpServletResponse response = getResource("/v1/topic-subtopics/" + topicSubtopic.getPublicId());
        TopicSubtopics.TopicSubtopicIndexDocument topicSubtopicIndexDocument = getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
        assertTrue(topicSubtopicIndexDocument.primary);
    }

    @Test
    public void deleted_primary_parent_topic_is_replaced() throws Exception {
        Topic subtopic = builder.topic();
        Topic primary = builder.topic(t -> t.name("primary").subtopic(subtopic));
        builder.topic(t -> t.name("other").subtopic(subtopic));
        subtopic.setPrimaryParentTopic(primary);

        deleteResource("/v1/topics/" + primary.getPublicId());

        assertEquals("other", subtopic.getPrimaryParentTopic().getName());
    }

    @Test
    public void setting_new_parent_makes_old_one_secondary() throws Exception {
        Topic subtopic = builder.topic(t -> t.name("a subtopic"));

        builder.topic("electricity", t -> t
                .name("electricity")
                .subtopic(subtopic)
        );

        Topic newPrimary = builder.topic("wiring", t -> t
                .name("Wiring")
        );

        createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
            topicid = newPrimary.getPublicId();
            subtopicid = subtopic.getPublicId();
            primary = true;
        }});

        subtopic.parentTopics.forEach(topicResource -> {
            Topic topic = topicResource.getTopic();
            if (topic.equals(newPrimary)) assertTrue(topicResource.isPrimary());
            else assertFalse(topicResource.isPrimary());
        });

    }

    @Test
    public void setting_secondary_parent_primary_makes_old_primary_parent_secondary() throws Exception {
        Topic subtopic = builder.topic(t -> t.name("a subtopic"));

        Topic oldprimary = builder.topic("electricity", t -> t
                .name("electricity")
                .subtopic(subtopic)
        );

        Topic newPrimary = builder.topic("wiring", t -> t
                .name("Wiring")
        );

        createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
            topicid = newPrimary.getPublicId();
            subtopicid = subtopic.getPublicId();
            primary = true;
        }});

        TopicSubtopic topicSubtopic = oldprimary.subtopics.iterator().next();

        updateResource("/v1/topic-subtopics/" + topicSubtopic.getPublicId().toString(), new TopicSubtopics.AddSubtopicToTopicCommand() {{
            primary = true;
        }});

        subtopic.parentTopics.forEach(topicResource -> {
            Topic topic = topicResource.getTopic();
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
        MockHttpServletResponse response = getResource(("/v1/topic-subtopics"));
        TopicSubtopics.TopicSubtopicIndexDocument[] subtopics = getObject(TopicSubtopics.TopicSubtopicIndexDocument[].class, response);

        assertAllTrue(subtopics, st -> st.rank == 0);
    }

    @Test
    public void subtopics_can_be_created_with_rank() throws Exception {
        Subject subject = builder.subject(s -> s.name("Subject").publicId("urn:subject:1"));
        Topic electricity = builder.topic(s -> s
                .name("Electricity")
                .publicId("urn:topic:1"));
        save(subject.addTopic(electricity));
        Topic alternatingCurrents = builder.topic(t -> t
                .name("Alternating currents")
                .publicId("urn:topic:11"));
        Topic wiring = builder.topic(t -> t
                .name("Wiring")
                .publicId("urn:topic:12"));

        createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
            topicid = electricity.getPublicId();
            subtopicid = alternatingCurrents.getPublicId();
            rank = 2;
        }});

        createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
            topicid = electricity.getPublicId();
            subtopicid = wiring.getPublicId();
            rank = 1;
        }});

        MockHttpServletResponse response = getResource("/v1/subjects/" + subject.getPublicId() + "/topics?recursive=true");
        TopicSubtopics.TopicSubtopicIndexDocument[] topics = getObject(TopicSubtopics.TopicSubtopicIndexDocument[].class, response);

        assertEquals(electricity.getPublicId(), topics[0].id);
        assertEquals(wiring.getPublicId(), topics[1].id);
        assertEquals(alternatingCurrents.getPublicId(), topics[2].id);
    }
}
