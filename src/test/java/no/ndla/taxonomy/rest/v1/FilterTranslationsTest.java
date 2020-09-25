package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.service.dtos.FilterDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FilterTranslationsTest extends RestTest {


    @Test
    public void can_get_single_filter() throws Exception {
        URI id = builder.filter(f -> f
                .name("Tømrer")
                .translation("nn", l -> l
                        .name("Tømrar")
                )
        ).getPublicId();

        final var resource = getFilterIndexDocument(id, "nn");
        assertEquals("Tømrar", resource.getName());
    }

    private FilterDTO getFilterIndexDocument(URI id, String language) throws Exception {
        String path = "/v1/filters/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return testUtils.getObject(FilterDTO.class, testUtils.getResource(path));
    }

    @Test
    public void can_get_all_translations_for_filters() throws Exception {
        builder.filter(f -> f
                .name("Tømrer")
                .translation("nn", l -> l
                        .name("Tømrar")
                )
        );
        builder.filter(f -> f.name("Negative tall").translation("nn", l -> l.name("Negative tal")));

        MockHttpServletResponse response = testUtils.getResource("/v1/filters?language=nn");
        final var filters = testUtils.getObject(FilterDTO[].class, response);

        assertAnyTrue(filters, f -> f.getName().equals("Tømrar"));
        assertAnyTrue(filters, f -> f.getName().equals("Negative tal"));
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.filter(f -> f
                .name("Tømrer")
        ).getPublicId();

        final var resource = getFilterIndexDocument(id, "nn");
        assertEquals("Tømrer", resource.getName());
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.filter(f -> f
                .name("Tømrer")
                .translation("nn", l -> l.name("Tømrar"))
        ).getPublicId();

        final var resource = getFilterIndexDocument(id, null);
        assertEquals("Tømrer", resource.getName());
    }

    @Test
    public void can_add_translation() throws Exception {
        Filter filter = builder.filter(f -> f
                .name("Tømrer")
                .publicId("urn:filter:1")
        );

        testUtils.updateResource("/v1/filters/urn:filter:1/translations/nn", new FilterTranslations.UpdateFilterTranslationCommand() {{
            name = "Tømrar";
        }});

        assertEquals("Tømrar", filter.getTranslation("nn").orElseThrow().getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        Filter filter = builder.filter(f -> f
                .name("Tømrar")
                .translation("nb", l -> l
                        .name("Tømrer")
                )
        );
        URI id = filter.getPublicId();

        testUtils.deleteResource("/v1/filters/" + id + "/translations/nb");

        assertNull(filter.getTranslation("nb").orElse(null));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        Filter filter = builder.filter(f -> f
                .name("Tømrer")
                .translation("nn", l -> l.name("Tømrar"))
                .translation("en", l -> l.name("Carpenter"))
                .translation("de", l -> l.name("Zimmermann"))
        );
        URI id = filter.getPublicId();

        FilterTranslations.FilterTranslationIndexDocument[] translations = testUtils.getObject(FilterTranslations.FilterTranslationIndexDocument[].class, testUtils.getResource("/v1/filters/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Tømrar") && t.language.equals("nn"));
        assertAnyTrue(translations, t -> t.name.equals("Carpenter") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Zimmermann") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        Filter filter = builder.filter(f -> f
                .name("Tømrar")
                .translation("nb", l -> l.name("Tømrer"))
        );
        URI id = filter.getPublicId();

        FilterTranslations.FilterTranslationIndexDocument translation = testUtils.getObject(FilterTranslations.FilterTranslationIndexDocument.class,
                testUtils.getResource("/v1/filters/" + id + "/translations/nb"));
        assertEquals("Tømrer", translation.name);
        assertEquals("nb", translation.language);
    }
}