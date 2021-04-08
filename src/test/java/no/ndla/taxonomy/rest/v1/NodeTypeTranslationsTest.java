package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.NodeType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NodeTypeTranslationsTest extends RestTest {

    @Test
    public void can_get_all_node_types() throws Exception {
        builder.nodeType(t -> t.name("Loose types").translation("nb", l -> l.name("Løse typer")));
        builder.nodeType(t -> t.name("Other type").translation("nb", l -> l.name("Annen type")));

        MockHttpServletResponse response = testUtils.getResource("/v1/nodetypes?language=nb");
        final var nodeTypes = testUtils.getObject(NodeTypes.NodeTypeIndexDocument[].class, response);

        assertTrue(2 <= nodeTypes.length);
        assertAnyTrue(nodeTypes, s -> s.name.equals("Løse typer"));
        assertAnyTrue(nodeTypes, s -> s.name.equals("Annen type"));
    }

    @Test
    public void can_get_single_node_type() throws Exception {
        URI id = builder.nodeType(t -> t
                .name("Loose types")
                .translation("nb", l -> l
                        .name("Løse typer")
                )
        ).getPublicId();

        final var nodeType = getNodeType(id, "nb");
        assertEquals("Løse typer", nodeType.name);
    }


    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.nodeType(t -> t
                .name("Loose types")
        ).getPublicId();
        final var nodeType = getNodeType(id, "XX");
        assertEquals("Loose types", nodeType.name);
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.nodeType(t -> t
                .name("Loose types")
                .translation("nb", l -> l
                        .name("Løse typer")
                )
        ).getPublicId();

        final var nodeType = getNodeType(id, null);
        assertEquals("Loose types", nodeType.name);
    }

    @Test
    public void can_add_translation() throws Exception {
        NodeType nodeType = builder.nodeType(t -> t.name("Loose types"));
        URI id = nodeType.getPublicId();

        testUtils.updateResource("/v1/nodetypes/" + id + "/translations/nb", new NodeTypeTranslations.UpdateNodeTypeTranslationCommand() {{
            name = "Løse typer";
        }});

        assertEquals("Løse typer", nodeType.getTranslation("nb").get().getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        NodeType nodeType = builder.nodeType(t -> t
                .name("Loose types")
                .translation("nb", l -> l
                        .name("Løse typer")
                )
        );
        URI id = nodeType.getPublicId();

        testUtils.deleteResource("/v1/nodetypes/" + id + "/translations/nb");

        assertNull(nodeType.getTranslation("nb").orElse(null));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        NodeType nodeType = builder.nodeType(t -> t
                .name("Default")
                .translation("nb", l -> l.name("Norsk"))
                .translation("en", l -> l.name("English"))
                .translation("de", l -> l.name("Deutch"))
        );
        URI id = nodeType.getPublicId();

        NodeTypeTranslations.NodeTypeTranslationIndexDocument[] translations = testUtils.getObject(NodeTypeTranslations.NodeTypeTranslationIndexDocument[].class, testUtils.getResource("/v1/nodetypes/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Norsk") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("English") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Deutch") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        NodeType nodeType = builder.nodeType(t -> t
                .name("Loose types")
                .translation("nb", l -> l.name("Løse typer"))
        );
        URI id = nodeType.getPublicId();

        NodeTypeTranslations.NodeTypeTranslationIndexDocument translation = testUtils.getObject(NodeTypeTranslations.NodeTypeTranslationIndexDocument.class,
                testUtils.getResource("/v1/nodetypes/" + id + "/translations/nb"));
        assertEquals("Løse typer", translation.name);
        assertEquals("nb", translation.language);
    }

    private NodeTypes.NodeTypeIndexDocument getNodeType(URI id, String language) throws Exception {
        String path = "/v1/nodetypes/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return testUtils.getObject(NodeTypes.NodeTypeIndexDocument.class, testUtils.getResource(path));
    }

}
