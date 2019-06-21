package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Filter;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FilterTranslationsTest extends RestTest {


    @Test
    public void can_get_single_filter() throws Exception {
        URI id = builder.filter(f -> f
                .name("Tømrer")
                .translation("nn", l -> l
                        .name("Tømrar")
                )
        ).getPublicId();

        Filters.FilterIndexDocument resource = getFilterIndexDocument(id, "nn");
        assertEquals("Tømrar", resource.name);
    }

    private Filters.FilterIndexDocument getFilterIndexDocument(URI id, String language) throws Exception {
        String path = "/v1/filters/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return getObject(Filters.FilterIndexDocument.class, getResource(path));
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

        MockHttpServletResponse response = getResource("/v1/filters?language=nn");
        Filters.FilterIndexDocument[] filters = getObject(Filters.FilterIndexDocument[].class, response);

        assertAnyTrue(filters, f -> f.name.equals("Tømrar"));
        assertAnyTrue(filters, f -> f.name.equals("Negative tal"));
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.filter(f -> f
                .name("Tømrer")
        ).getPublicId();

        Filters.FilterIndexDocument resource = getFilterIndexDocument(id, "nn");
        assertEquals("Tømrer", resource.name);
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.filter(f -> f
                .name("Tømrer")
                .translation("nn", l -> l.name("Tømrar"))
        ).getPublicId();

        Filters.FilterIndexDocument resource = getFilterIndexDocument(id, null);
        assertEquals("Tømrer", resource.name);
    }

    @Test
    public void can_add_translation() throws Exception {
        Filter filter = builder.filter(f -> f
                .name("Tømrer")
                .publicId("urn:filter:1")
        );

        updateResource("/v1/filters/urn:filter:1/translations/nn", new FilterTranslations.UpdateFilterTranslationCommand() {{
            name = "Tømrar";
        }});

        assertEquals("Tømrar", filter.getTranslation("nn").get().getName());
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

        deleteResource("/v1/filters/" + id + "/translations/nb");

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

        FilterTranslations.FilterTranslationIndexDocument[] translations = getObject(FilterTranslations.FilterTranslationIndexDocument[].class, getResource("/v1/filters/" + id + "/translations"));

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

        FilterTranslations.FilterTranslationIndexDocument translation = getObject(FilterTranslations.FilterTranslationIndexDocument.class,
                getResource("/v1/filters/" + id + "/translations/nb"));
        assertEquals("Tømrer", translation.name);
        assertEquals("nb", translation.language);
    }
}