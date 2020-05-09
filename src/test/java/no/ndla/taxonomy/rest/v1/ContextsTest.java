package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.rest.v1.dtos.topics.TopicWithPathsIndexDocument;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.*;

public class ContextsTest extends RestTest {
    @Autowired
    private CachedUrlUpdaterService cachedUrlUpdaterService;

    @BeforeEach
    void cleanDatabase() {
        subjectRepository.deleteAllAndFlush();
        topicRepository.deleteAllAndFlush();
    }

    @Test
    public void all_subjects_are_contexts() throws Exception {
        subjectRepository.flush();
        topicRepository.flush();

        builder.subject(s -> s
                .publicId("urn:subject:1")
                .name("Subject 1")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/contexts");
        Contexts.ContextIndexDocument[] contexts = testUtils.getObject(Contexts.ContextIndexDocument[].class, response);

        assertEquals(1, contexts.length);
        assertEquals("urn:subject:1", contexts[0].id.toString());
        assertEquals("/subject:1", contexts[0].path);
        assertEquals("Subject 1", contexts[0].name);
    }


    @Test
    public void topics_can_be_contexts() throws Exception {
        builder.topic(t -> t
                .publicId("urn:topic:1")
                .name("Topic 1")
                .isContext(true)
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/contexts");
        Contexts.ContextIndexDocument[] contexts = testUtils.getObject(Contexts.ContextIndexDocument[].class, response);

        assertEquals(1, contexts.length);
        assertEquals("urn:topic:1", contexts[0].id.toString());
        assertEquals("/topic:1", contexts[0].path);
        assertEquals("Topic 1", contexts[0].name);
    }

    @Test
    public void can_add_topic_as_context() throws Exception {
        Topic topic = builder.topic(t -> t
                .publicId("urn:topic:ct:2")
        );

        testUtils.createResource("/v1/contexts", new Contexts.CreateContextCommand() {{
            id = topic.getPublicId();
        }});

        assertTrue(topic.isContext());
    }


    @Test
    public void can_remove_topic_as_context() throws Exception {
        Topic topic = builder.topic(t -> t
                .publicId("urn:topic:1")
                .isContext(true)
        );

        testUtils.deleteResource("/v1/contexts/urn:topic:1");
        assertFalse(topic.isContext());
    }


    @Test
    public void can_get_translated_contexts() throws Exception {
        subjectRepository.deleteAllAndFlush();
        topicRepository.deleteAllAndFlush();

        builder.subject(s -> s
                .publicId("urn:subject:1")
                .name("Subject 1")
                .translation("nb", tr -> tr.name("Fag 1"))
        );

        builder.topic(t -> t
                .publicId("urn:topic:1")
                .name("Topic 1")
                .translation("nb", tr -> tr.name("Emne 1"))
                .isContext(true)
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/contexts?language=nb");
        Contexts.ContextIndexDocument[] contexts = testUtils.getObject(Contexts.ContextIndexDocument[].class, response);

        assertEquals(2, contexts.length);

        assertAnyTrue(contexts, c -> c.name.equals("Emne 1"));
        assertAnyTrue(contexts, c -> c.name.equals("Fag 1"));
    }

    @Test
    public void root_context_is_more_important_than_primary_parent() throws Exception {
        Topic topic = builder.topic(t -> t
                .publicId("urn:topic:1")
        );

        Subject subject = builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(topic)
        );

        topic.setContext(true);
        topicRepository.saveAndFlush(topic);

        cachedUrlUpdaterService.updateCachedUrls(topic);

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1");
        TopicWithPathsIndexDocument topicIndexDocument = testUtils.getObject(TopicWithPathsIndexDocument.class, response);
        assertEquals("/topic:1", topicIndexDocument.path);
    }
}
