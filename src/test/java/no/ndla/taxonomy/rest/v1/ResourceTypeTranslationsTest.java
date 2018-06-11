package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.ResourceType;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ResourceTypeTranslationsTest extends RestTest {

    @Test
    public void can_get_all_resource_types() throws Exception {
        builder.resourceType(t -> t.name("Article").translation("nb", l -> l.name("Artikkel")));
        builder.resourceType(t -> t.name("Lecture").translation("nb", l -> l.name("Forelesning")));

        MockHttpServletResponse response = getResource("/v1/resource-types?language=nb");
        ResourceTypes.ResourceTypeIndexDocument[] resourceTypes = getObject(ResourceTypes.ResourceTypeIndexDocument[].class, response);

        assertEquals(2, resourceTypes.length);
        assertAnyTrue(resourceTypes, s -> s.name.equals("Artikkel"));
        assertAnyTrue(resourceTypes, s -> s.name.equals("Forelesning"));
    }

    @Test
    public void can_get_single_resource_type() throws Exception {
        URI id = builder.resourceType(t -> t
                .name("Article")
                .translation("nb", l -> l
                        .name("Artikkel")
                )
        ).getPublicId();

        ResourceTypes.ResourceTypeIndexDocument resourceType = getResourceTypeIndexDocument(id, "nb");
        assertEquals("Artikkel", resourceType.name);
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.resourceType(t -> t
                .name("Article")
        ).getPublicId();
        ResourceTypes.ResourceTypeIndexDocument resourceType = getResourceTypeIndexDocument(id, "XX");
        assertEquals("Article", resourceType.name);
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.resourceType(t -> t
                .name("Article")
                .translation("nb", l -> l
                        .name("Artikkel")
                )
        ).getPublicId();

        ResourceTypes.ResourceTypeIndexDocument resourceType = getResourceTypeIndexDocument(id, null);
        assertEquals("Article", resourceType.name);
    }

    @Test
    public void can_add_translation() throws Exception {
        ResourceType article = builder.resourceType(t -> t.name("Article"));
        URI id = article.getPublicId();

        updateResource("/v1/resource-types/" + id + "/translations/nb", new ResourceTypeTranslations.UpdateResourceTypeTranslationCommand() {{
            name = "Artikkel";
        }});

        assertEquals("Artikkel", article.getTranslation("nb").getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        ResourceType resourceType = builder.resourceType(t -> t
                .name("Article")
                .translation("nb", l -> l
                        .name("Artikkel")
                )
        );
        URI id = resourceType.getPublicId();

        deleteResource("/v1/resource-types/" + id + "/translations/nb");

        assertNull(resourceType.getTranslation("nb"));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        ResourceType resourceType = builder.resourceType(t -> t
                .name("Article")
                .translation("nb", l -> l.name("Artikkel"))
                .translation("en", l -> l.name("Article"))
                .translation("de", l -> l.name("Artikel"))
        );
        URI id = resourceType.getPublicId();

        ResourceTypeTranslations.ResourceTypeTranslationIndexDocument[] translations =
                getObject(ResourceTypeTranslations.ResourceTypeTranslationIndexDocument[].class,
                        getResource("/v1/resource-types/" + id + "/translations")
                );

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Artikkel") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Article") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Artikel") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        ResourceType resourceType = builder.resourceType(t -> t
                .name("Article")
                .translation("nb", l -> l.name("Artikkel"))
        );
        URI id = resourceType.getPublicId();

        ResourceTypeTranslations.ResourceTypeTranslationIndexDocument translation = getObject(ResourceTypeTranslations.ResourceTypeTranslationIndexDocument.class,
                getResource("/v1/resource-types/" + id + "/translations/nb"));
        assertEquals("Artikkel", translation.name);
        assertEquals("nb", translation.language);
    }

    private ResourceTypes.ResourceTypeIndexDocument getResourceTypeIndexDocument(URI id, String language) throws Exception {
        String path = "/v1/resource-types/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return getObject(ResourceTypes.ResourceTypeIndexDocument.class, getResource(path));
    }

}
