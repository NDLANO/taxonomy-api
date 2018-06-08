package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Resource;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ResourceTranslationsTest extends RestTest {

    @Test
    public void can_get_all_resources_with_translation() throws Exception {
        builder.resource(r -> r.name("The inner planets").translation("nb", tr -> tr.name("De indre planetene")));
        builder.resource(r -> r.name("Gas giants").translation("nb", tr -> tr.name("Gasskjemper")));

        MockHttpServletResponse response = getResource("/v1/resources?language=nb");
        Resources.ResourceIndexDocument[] resources = getObject(Resources.ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);
        assertAnyTrue(resources, s -> "De indre planetene".equals(s.name));
        assertAnyTrue(resources, s -> "Gasskjemper".equals(s.name));
    }

    @Test
    public void can_get_single_resource_with_translation() throws Exception {
        URI trigonometry = builder.resource(s -> s
                .name("introduction to trigonometry")
                .translation("nb", tr -> tr.name("Introduksjon til trigonometri"))
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/resources/" + trigonometry + "?language=nb");
        Resources.ResourceIndexDocument resource = getObject(Resources.ResourceIndexDocument.class, response);

        assertEquals("Introduksjon til trigonometri", resource.name);
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.resource(t -> t
                .name("Introduction to algrebra")
        ).getPublicId();
        Resources.ResourceIndexDocument resource = getResourceIndexDocument(id, "XX");
        assertEquals("Introduction to algrebra", resource.name);
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.resource(t -> t
                .name("Introduction to algrebra")
                .translation("nb", l -> l
                        .name("Introduksjon til algebra")
                )
        ).getPublicId();

        Resources.ResourceIndexDocument resource = getResourceIndexDocument(id, null);
        assertEquals("Introduction to algrebra", resource.name);
    }

    @Test
    public void can_add_translation() throws Exception {
        Resource resource = builder.resource(t -> t.name("Introduction to algrebra"));
        URI id = resource.getPublicId();

        updateResource("/v1/resources/" + id + "/translations/nb", new ResourceTranslations.UpdateResourceTranslationCommand() {{
            name = "Introduksjon til algebra";
        }});

        assertEquals("Introduksjon til algebra", resource.getTranslation("nb").getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        Resource resource = builder.resource(t -> t
                .name("Introduction to algrebra")
                .translation("nb", l -> l
                        .name("Introduksjon til algebra")
                )
        );
        URI id = resource.getPublicId();

        deleteResource("/v1/resources/" + id + "/translations/nb");

        assertNull(resource.getTranslation("nb"));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        Resource resource = builder.resource(t -> t
                .name("Introduction to algrebra")
                .translation("nb", l -> l.name("Introduksjon til algebra"))
                .translation("en", l -> l.name("Introduction to algrebra"))
                .translation("de", l -> l.name("Introduktion bis Algebra"))
        );
        URI id = resource.getPublicId();

        ResourceTranslations.ResourceTranslationIndexDocument[] translations = getObject(ResourceTranslations.ResourceTranslationIndexDocument[].class, getResource("/v1/resources/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Introduksjon til algebra") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Introduction to algrebra") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Introduktion bis Algebra") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        Resource resource = builder.resource(t -> t
                .name("Introduction to algrebra")
                .translation("nb", l -> l.name("Introduksjon til algebra"))
        );
        URI id = resource.getPublicId();

        ResourceTranslations.ResourceTranslationIndexDocument translation = getObject(ResourceTranslations.ResourceTranslationIndexDocument.class,
                getResource("/v1/resources/" + id + "/translations/nb"));
        assertEquals("Introduksjon til algebra", translation.name);
        assertEquals("nb", translation.language);
    }

    private Resources.ResourceIndexDocument getResourceIndexDocument(URI id, String language) throws Exception {
        String path = "/v1/resources/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return getObject(Resources.ResourceIndexDocument.class, getResource(path));
    }

}
