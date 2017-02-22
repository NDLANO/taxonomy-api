package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.Iterator;

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

        TopicSubtopics.UpdateTopicSubtopicCommand command = new TopicSubtopics.UpdateTopicSubtopicCommand();
        command.primary = true;
        updateResource("/v1/topic-subtopics/" + id, command);

        assertTrue(topicSubtopicRepository.getByPublicId(id).isPrimary());
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
    public void subtopic_can_only_have_one_primary_topic_connection() throws Exception {
        builder.topic("electricity", t -> t
                .name("electricity")
                .subtopic("ac", st -> st
                        .name("alternating current")
                )
        );

        builder.topic("wiring", t -> t
                .name("Wiring")
        );
        URI id = getId(
                createResource("/v1/topic-subtopics", new TopicSubtopics.AddSubtopicToTopicCommand() {{
                    topicid = builder.topic("wiring").getPublicId();
                    subtopicid = builder.topic("ac").getPublicId();
                    primary = true;
                }})
        );

        MockHttpServletResponse response = getResource("/v1/topic-subtopics/" + id);
        TopicSubtopics.TopicSubtopicIndexDocument topicSubtopicIndexDocument = getObject(TopicSubtopics.TopicSubtopicIndexDocument.class, response);
        assertTrue(topicSubtopicIndexDocument.primary);

        Topic topic = builder.topic("ac");
        Iterator<TopicSubtopic> iterator = topic.getSubtopicConnections();
        while (iterator.hasNext()) {
            TopicSubtopic topicSubtopic = iterator.next();
            if (!id.equals(topicSubtopic.getPublicId())) {
                assertFalse(topicSubtopic.isPrimary());
            }
        }

    }
}
