/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.rest.v1.dtos.ResourceTypeDTO;
import no.ndla.taxonomy.rest.v1.dtos.TranslationPUT;
import no.ndla.taxonomy.service.dtos.TranslationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ResourceTypeTranslationsTest extends RestTest {

    @Test
    public void can_get_all_resource_types() throws Exception {
        builder.resourceType(t -> t.name("Article").translation("Artikkel", "nb"));
        builder.resourceType(t -> t.name("Lecture").translation("Forelesning", "nb"));

        MockHttpServletResponse response = testUtils.getResource("/v1/resource-types?language=nb");
        ResourceTypeDTO[] resourceTypes = testUtils.getObject(ResourceTypeDTO[].class, response);

        assertTrue(resourceTypes.length >= 2);
        assertAnyTrue(resourceTypes, s -> s.name.equals("Artikkel"));
        assertAnyTrue(resourceTypes, s -> s.name.equals("Forelesning"));
    }

    @Test
    public void can_get_single_resource_type() throws Exception {
        URI id = builder.resourceType(t -> t.name("Article").translation("Artikkel", "nb"))
                .getPublicId();

        ResourceTypeDTO resourceType = getResourceTypeIndexDocument(id, "nb");
        assertEquals("Artikkel", resourceType.name);
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.resourceType(t -> t.name("Article")).getPublicId();
        ResourceTypeDTO resourceType = getResourceTypeIndexDocument(id, "XX");
        assertEquals("Article", resourceType.name);
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.resourceType(t -> t.name("Article").translation("Artikkel", "nb"))
                .getPublicId();

        ResourceTypeDTO resourceType = getResourceTypeIndexDocument(id, null);
        assertEquals("Article", resourceType.name);
    }

    @Test
    public void can_add_translation() throws Exception {
        ResourceType article = builder.resourceType(t -> t.name("Article"));
        URI id = article.getPublicId();

        testUtils.updateResource("/v1/resource-types/" + id + "/translations/nb", new TranslationPUT() {
            {
                name = "Artikkel";
            }
        });

        assertEquals("Artikkel", article.getTranslation("nb").get().getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        ResourceType resourceType = builder.resourceType(t -> t.name("Article").translation("Artikkel", "nb"));
        URI id = resourceType.getPublicId();

        testUtils.deleteResource("/v1/resource-types/" + id + "/translations/nb");

        assertNull(resourceType.getTranslation("nb").orElse(null));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        ResourceType resourceType = builder.resourceType(t -> t.name("Article")
                .translation("Artikkel", "nb")
                .translation("Article", "en")
                .translation("Artikel", "de"));
        URI id = resourceType.getPublicId();

        TranslationDTO[] translations = testUtils.getObject(
                TranslationDTO[].class, testUtils.getResource("/v1/resource-types/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Artikkel") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Article") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Artikel") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        ResourceType resourceType = builder.resourceType(t -> t.name("Article").translation("Artikkel", "nb"));
        URI id = resourceType.getPublicId();

        TranslationDTO translation = testUtils.getObject(
                TranslationDTO.class, testUtils.getResource("/v1/resource-types/" + id + "/translations/nb"));
        assertEquals("Artikkel", translation.name);
        assertEquals("nb", translation.language);
    }

    private ResourceTypeDTO getResourceTypeIndexDocument(URI id, String language) throws Exception {
        String path = "/v1/resource-types/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return testUtils.getObject(ResourceTypeDTO.class, testUtils.getResource(path));
    }
}
