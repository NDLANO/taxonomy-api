/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.dtos.EntityWithPathDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
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
        nodeRepository.deleteAllAndFlush();
    }

    @Test
    public void all_subjects_are_contexts() throws Exception {
        nodeRepository.flush();

        Version version = versionService.getPublished().get();
        builder.node(version,
                s -> s.nodeType(NodeType.SUBJECT).isContext(true).publicId("urn:subject:1").name("Subject 1"));

        MockHttpServletResponse response = testUtils.getResource("/v1/contexts");
        Contexts.ContextIndexDocument[] contexts = testUtils.getObject(Contexts.ContextIndexDocument[].class, response);

        assertEquals(1, contexts.length);
        assertEquals("urn:subject:1", contexts[0].id.toString());
        assertEquals("/subject:1", contexts[0].path);
        assertEquals("Subject 1", contexts[0].name);
    }

    @Test
    public void topics_can_be_contexts() throws Exception {
        Version version = versionService.getPublished().get();
        builder.node(version, t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:1").name("Topic 1").isContext(true));

        MockHttpServletResponse response = testUtils.getResource("/v1/contexts");
        Contexts.ContextIndexDocument[] contexts = testUtils.getObject(Contexts.ContextIndexDocument[].class, response);

        assertEquals(1, contexts.length);
        assertEquals("urn:topic:1", contexts[0].id.toString());
        assertEquals("/topic:1", contexts[0].path);
        assertEquals("Topic 1", contexts[0].name);
    }

    @Test
    public void can_add_topic_as_context() throws Exception {
        Version version = versionService.getPublished().get();
        Node topic = builder.node(version, t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:ct:2"));

        testUtils.createResource("/v1/contexts", new Contexts.CreateContextCommand() {
            {
                id = topic.getPublicId();
            }
        });

        assertTrue(topic.isContext());
    }

    @Test
    public void can_remove_topic_as_context() throws Exception {
        Version version = versionService.getPublished().get();
        Node topic = builder.node(version, t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:1").isContext(true));

        testUtils.deleteResource("/v1/contexts/urn:topic:1");
        assertFalse(topic.isContext());
    }

    @Test
    public void can_get_translated_contexts() throws Exception {
        nodeRepository.deleteAllAndFlush();
        Version version = versionService.getPublished().get();
        builder.node(version, s -> s.nodeType(NodeType.SUBJECT).isContext(true).publicId("urn:subject:1")
                .name("Subject 1").translation("nb", tr -> tr.name("Fag 1")));

        builder.node(version, t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:1").name("Topic 1")
                .translation("nb", tr -> tr.name("Emne 1")).isContext(true));

        MockHttpServletResponse response = testUtils.getResource("/v1/contexts?language=nb");
        Contexts.ContextIndexDocument[] contexts = testUtils.getObject(Contexts.ContextIndexDocument[].class, response);

        assertEquals(2, contexts.length);

        assertAnyTrue(contexts, c -> c.name.equals("Emne 1"));
        assertAnyTrue(contexts, c -> c.name.equals("Fag 1"));
    }

    // TODO Set is not ordered
    @Test
    public void root_context_is_more_important_than_primary_parent() throws Exception {
        Version version = versionService.getPublished().get();
        Node topic = builder.node(version, t -> t.nodeType(NodeType.TOPIC).publicId("urn:topic:1"));

        Node subject = builder.node(version,
                s -> s.nodeType(NodeType.SUBJECT).isContext(true).publicId("urn:subject:1").child(topic));

        topic.setContext(true);
        nodeRepository.saveAndFlush(topic);

        cachedUrlUpdaterService.updateCachedUrls(topic);

        MockHttpServletResponse response = testUtils.getResource("/v1/topics/urn:topic:1");
        final var topicIndexDocument = testUtils.getObject(NodeDTO.class, response);
        // assertEquals("/topic:1", topicIndexDocument.getPath());
        assertAnyTrue(topicIndexDocument.getPaths(), p -> p.equals("/topic:1"));
    }
}
