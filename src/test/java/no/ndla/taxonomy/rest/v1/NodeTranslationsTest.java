package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import no.ndla.taxonomy.service.dtos.NodeDTO;
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
    public void can_get_all_node_types() throws Exception {
        builder.topic(t -> t.name("Trigonometry").translation("nb", l -> l.name("Trigonometri")));
        builder.topic(t -> t.name("Integration").translation("nb", l -> l.name("Integrasjon")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes?language=nb");
        final var nodes = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(2, nodes.length);
        assertAnyTrue(nodes, s -> s.getName().equals("Trigonometri"));
        assertAnyTrue(nodes, s -> s.getName().equals("Integrasjon"));
    }

    @Test
    public void can_get_single_node() throws Exception {
        URI id = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l
                        .name("Trigonometri")
                )
        ).getPublicId();

        final var node = getNode(id, "nb");
        assertEquals("Trigonometri", node.getName());
    }


    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.topic(t -> t
                .name("Trigonometry")
        ).getPublicId();
        final var node = getNode(id, "XX");
        assertEquals("Trigonometry", node.getName());
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l
                        .name("Trigonometri")
                )
        ).getPublicId();

        final var node = getNode(id, null);
        assertEquals("Trigonometry", node.getName());
    }

    @Test
    public void can_add_translation() throws Exception {
        Topic trigonometry = builder.topic(t -> t.name("Trigonometry"));
        URI id = trigonometry.getPublicId();

        testUtils.updateResource("/v1/nodes/" + id + "/translations/nb", new NodeTranslations.UpdateNodeTranslationCommand() {{
            name = "Trigonometri";
        }});

        assertEquals("Trigonometri", trigonometry.getTranslation("nb").get().getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        Topic topic = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l
                        .name("Trigonometri")
                )
        );
        URI id = topic.getPublicId();

        testUtils.deleteResource("/v1/nodes/" + id + "/translations/nb");

        assertNull(topic.getTranslation("nb").orElse(null));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        Topic topic = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l.name("Trigonometri"))
                .translation("en", l -> l.name("Trigonometry"))
                .translation("de", l -> l.name("Trigonometrie"))
        );
        URI id = topic.getPublicId();

        NodeTranslations.NodeTranslationIndexDocument[] translations = testUtils.getObject(NodeTranslations.NodeTranslationIndexDocument[].class, testUtils.getResource("/v1/nodes/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Trigonometri") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Trigonometry") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Trigonometrie") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        Topic topic = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l.name("Trigonometri"))
        );
        URI id = topic.getPublicId();

        NodeTranslations.NodeTranslationIndexDocument translation = testUtils.getObject(NodeTranslations.NodeTranslationIndexDocument.class,
                testUtils.getResource("/v1/nodes/" + id + "/translations/nb"));
        assertEquals("Trigonometri", translation.name);
        assertEquals("nb", translation.language);
    }


    @Test
    public void can_get_resources_for_a_node_recursively_with_translation() throws Exception {
        builder.resourceType("article", rt -> rt.name("Article").translation("nb", tr -> tr.name("Artikkel")));

        URI a = builder.topic(t -> t
                .resource(r -> r
                        .name("Introduction to calculus")
                        .translation("nb", tr -> tr.name("Introduksjon til calculus"))
                        .resourceType("article")
                )
                .subtopic(st -> st
                        .resource(r -> r
                                .name("Introduction to integration")
                                .translation("nb", tr -> tr.name("Introduksjon til integrasjon"))
                                .resourceType("article")
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/" + a + "/resources?recursive=true&language=nb");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "Introduksjon til calculus".equals(r.name));
        assertAnyTrue(result, r -> "Introduksjon til integrasjon".equals(r.name));
        assertAllTrue(result, r -> r.resourceTypes.iterator().next().getName().equals("Artikkel"));
    }

    @Test
    public void can_get_resources_for_a_node_without_child_node_resources_with_translation() throws Exception {
        builder.resourceType("article", rt -> rt.name("Article").translation("nb", tr -> tr.name("Artikkel")));

        builder.subject(s -> s
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r
                                .name("resource 1")
                                .translation("nb", tr -> tr.name("ressurs 1"))
                                .resourceType("article")
                        )
                        .resource(r -> r
                                .name("resource 2")
                                .translation("nb", tr -> tr.name("ressurs 2"))
                                .resourceType("article")
                        )
                        .subtopic(st -> st.name("subtopic").resource(r -> r.name("subtopic resource")))
                ));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodes/urn:topic:1/resources?language=nb");
        ResourceIndexDocument[] result = testUtils.getObject(ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "ressurs 1".equals(r.name));
        assertAnyTrue(result, r -> "ressurs 2".equals(r.name));
        assertAllTrue(result, r -> r.resourceTypes.iterator().next().getName().equals("Artikkel"));
    }

    private NodeDTO getNode(URI id, String language) throws Exception {
        String path = "/v1/nodes/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return testUtils.getObject(NodeDTO.class, testUtils.getResource(path));
    }

}
