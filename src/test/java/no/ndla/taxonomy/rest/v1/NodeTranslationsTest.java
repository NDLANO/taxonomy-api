/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.dtos.EntityWithPathChildDTO;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithNodeConnectionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAllTrue;
import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NodeTranslationsTest extends RestTest {

    @Test
    public void can_get_all_nodes() throws Exception {
        builder.node(NodeType.NODE, t -> t.name("Trigonometry").translation("nb", l -> l.name("Trigonometri")));
        builder.node(NodeType.NODE, t -> t.name("Integration").translation("nb", l -> l.name("Integrasjon")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes?language=nb");
        final var topics = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, s -> s.name.equals("Trigonometri"));
        assertAnyTrue(topics, s -> s.name.equals("Integrasjon"));
    }

    @Test
    public void can_get_single_node() throws Exception {
        URI id = builder.node(NodeType.NODE, t -> t.name("Trigonometry").translation("nb", l -> l.name("Trigonometri")))
                .getPublicId();

        final var topic = getNode(id, "nb");
        assertEquals("Trigonometri", topic.getName());
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.node(NodeType.TOPIC, t -> t.name("Trigonometry")).getPublicId();
        final var topic = getNode(id, "XX");
        assertEquals("Trigonometry", topic.getName());
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder
                .node(NodeType.TOPIC, t -> t.name("Trigonometry").translation("nb", l -> l.name("Trigonometri")))
                .getPublicId();

        final var topic = getNode(id, null);
        assertEquals("Trigonometry", topic.getName());
    }

    @Test
    public void can_add_translation() throws Exception {
        Node trigonometry = builder.node(NodeType.NODE, t -> t.name("Trigonometry"));
        URI id = trigonometry.getPublicId();

        testUtils.updateResource("/v1/nodes/" + id + "/translations/nb",
                new NodeTranslations.UpdateTranslationCommand() {
                    {
                        name = "Trigonometri";
                    }
                });

        assertEquals("Trigonometri", trigonometry.getTranslation("nb").get().getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        Node node = builder.node(NodeType.TOPIC,
                t -> t.name("Trigonometry").translation("nb", l -> l.name("Trigonometri")));
        URI id = node.getPublicId();

        testUtils.deleteResource("/v1/nodes/" + id + "/translations/nb");

        assertNull(node.getTranslation("nb").orElse(null));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        Node topic = builder.node(NodeType.TOPIC,
                t -> t.name("Trigonometry").translation("nb", l -> l.name("Trigonometri"))
                        .translation("en", l -> l.name("Trigonometry"))
                        .translation("de", l -> l.name("Trigonometrie")));
        URI id = topic.getPublicId();

        NodeTranslations.TranslationDTO[] translations = testUtils.getObject(NodeTranslations.TranslationDTO[].class,
                testUtils.getResource("/v1/nodes/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Trigonometri") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Trigonometry") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Trigonometrie") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        Node topic = builder.node(NodeType.TOPIC,
                t -> t.name("Trigonometry").translation("nb", l -> l.name("Trigonometri")));
        URI id = topic.getPublicId();

        NodeTranslations.TranslationDTO translation = testUtils.getObject(NodeTranslations.TranslationDTO.class,
                testUtils.getResource("/v1/nodes/" + id + "/translations/nb"));
        assertEquals("Trigonometri", translation.name);
        assertEquals("nb", translation.language);
    }

    @Test
    public void can_get_resources_for_a_node_recursively_with_translation() throws Exception {
        builder.resourceType("article", rt -> rt.name("Article").translation("nb", tr -> tr.name("Artikkel")));

        URI a = builder
                .node(NodeType.TOPIC,
                        t -> t.resource(r -> r.name("Introduction to calculus")
                                .translation("nb", tr -> tr.name("Introduksjon til calculus")).resourceType("article"))
                                .child(NodeType.TOPIC,
                                        st -> st.resource(r -> r.name("Introduction to integration")
                                                .translation("nb", tr -> tr.name("Introduksjon til integrasjon"))
                                                .resourceType("article"))))
                .getPublicId();

        MockHttpServletResponse response = testUtils
                .getResource("/v1/nodes/" + a + "/resources?recursive=true&language=nb");
        ResourceWithNodeConnectionDTO[] result = testUtils.getObject(ResourceWithNodeConnectionDTO[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "Introduksjon til calculus".equals(r.getName()));
        assertAnyTrue(result, r -> "Introduksjon til integrasjon".equals(r.getName()));
        assertAllTrue(result, r -> r.getResourceTypes().iterator().next().getName().equals("Artikkel"));
    }

    @Test
    public void can_get_resources_for_a_node_without_childrem_resources_with_translation() throws Exception {
        builder.resourceType("article", rt -> rt.name("Article").translation("nb", tr -> tr.name("Artikkel")));

        builder.node(NodeType.SUBJECT, s -> s.isContext(true).child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                .resource(
                        r -> r.name("resource 1").translation("nb", tr -> tr.name("ressurs 1")).resourceType("article"))
                .resource(
                        r -> r.name("resource 2").translation("nb", tr -> tr.name("ressurs 2")).resourceType("article"))
                .child(NodeType.TOPIC, st -> st.name("subtopic").resource(r -> r.name("subtopic resource")))));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1/resources?language=nb");
        ResourceWithNodeConnectionDTO[] result = testUtils.getObject(ResourceWithNodeConnectionDTO[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "ressurs 1".equals(r.getName()));
        assertAnyTrue(result, r -> "ressurs 2".equals(r.getName()));
        assertAllTrue(result, r -> r.getResourceTypes().iterator().next().getName().equals("Artikkel"));
    }

    private NodeDTO getNode(URI id, String language) throws Exception {
        String path = "/v1/nodes/" + id;
        if (isNotEmpty(language))
            path = path + "?language=" + language;
        return testUtils.getObject(NodeDTO.class, testUtils.getResource(path));
    }

}
