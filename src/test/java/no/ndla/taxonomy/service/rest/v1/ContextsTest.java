package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.Topic;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.junit.Assert.*;

public class ContextsTest extends RestTest {

    @Test
    public void all_subjects_are_contexts() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .name("Subject 1")
        );

        MockHttpServletResponse response = getResource("/v1/contexts");
        Contexts.ContextIndexDocument[] contexts = getObject(Contexts.ContextIndexDocument[].class, response);

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

        MockHttpServletResponse response = getResource("/v1/contexts");
        Contexts.ContextIndexDocument[] contexts = getObject(Contexts.ContextIndexDocument[].class, response);

        assertEquals(1, contexts.length);
        assertEquals("urn:topic:1", contexts[0].id.toString());
        assertEquals("/topic:1", contexts[0].path);
        assertEquals("Topic 1", contexts[0].name);
    }

    @Test
    public void can_add_topic_as_context() throws Exception {
        Topic topic = builder.topic(t -> t
                .publicId("urn:topic:1")
        );

        MockHttpServletResponse response = createResource("/v1/contexts", new Contexts.CreateContextCommand() {{
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

        MockHttpServletResponse response = deleteResource("/v1/contexts/urn:topic:1");
        assertFalse(topic.isContext());
    }


    @Test
    public void can_get_translated_contexts() throws Exception {
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

        MockHttpServletResponse response = getResource("/v1/contexts?language=nb");
        Contexts.ContextIndexDocument[] contexts = getObject(Contexts.ContextIndexDocument[].class, response);

        assertEquals(2, contexts.length);

        assertAnyTrue(contexts, c -> c.name.equals("Emne 1"));
        assertAnyTrue(contexts, c -> c.name.equals("Fag 1"));
    }

    /** TODO:
    @Test
    public void root_context_is_more_important_than_primary_parent() throws Exception {
        Topic topic = builder.topic(t -> t
                .publicId("urn:topic:1")
        );

        Subject subject = builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(topic)
        );

        topic.setPrimarySubject(subject);

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1");
        Topics.TopicIndexDocument topicIndexDocument = getObject(Topics.TopicIndexDocument.class, response);
        assertEquals("/topic:1", topicIndexDocument.path);
    }
    */
}
