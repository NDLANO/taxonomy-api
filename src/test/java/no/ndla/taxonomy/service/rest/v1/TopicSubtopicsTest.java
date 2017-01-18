package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
public class TopicSubtopicsTest extends RestTest {

    @Test
    public void can_add_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = newTopic().name("calculus").getPublicId();
        integrationId = newTopic().name("integration").getPublicId();

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
        URI integrationId, calculusId;
        Topic calculus = newTopic().name("calculus");
        Topic integration = newTopic().name("integration");
        calculus.addSubtopic(integration);

        calculusId = calculus.getPublicId();
        integrationId = integration.getPublicId();

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
        URI alternatingCurrentId, electricityId, calculusId, integrationId;

        Topic electricity = newTopic().name("electricity");
        Topic alternatingCurrent = newTopic().name("alternating current");
        save(electricity.addSubtopic(alternatingCurrent));

        Topic calculus = newTopic().name("calculus");
        Topic integration = newTopic().name("integration");
        save(calculus.addSubtopic(integration));

        electricityId = electricity.getPublicId();
        alternatingCurrentId = alternatingCurrent.getPublicId();
        calculusId = calculus.getPublicId();
        integrationId = integration.getPublicId();

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
}
