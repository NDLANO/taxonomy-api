package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Status;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StatusTranslationsTest extends RestTest {

    @Test
    public void can_get_all_statuses() throws Exception {
        builder.status(t -> t.name("Status A-A").translation("nb", l -> l.name("Status A nb")));
        builder.status(t -> t.name("Status B").translation("nb", l -> l.name("Status B nb")));

        MockHttpServletResponse response = testUtils.getResource("/v1/statuses?language=nb");
        Statuses.StatusIndexDocument[] statuses = testUtils.getObject(Statuses.StatusIndexDocument[].class, response);

        assertEquals(2, statuses.length);
        assertAnyTrue(statuses, s -> s.name.equals("Status A nb"));
        assertAnyTrue(statuses, s -> s.name.equals("Status B nb"));
    }

    @Test
    public void can_get_single_status() throws Exception {
        URI id = builder.status(t -> t
                .name("Status A")
                .translation("nb", l -> l
                        .name("A")
                )
        ).getPublicId();

        Statuses.StatusIndexDocument status = getStatusIndexDocument(id, "nb");
        assertEquals("A", status.name);
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.status(t -> t
                .name("Status A-A")
        ).getPublicId();
        Statuses.StatusIndexDocument status = getStatusIndexDocument(id, "XX");
        assertEquals("Status A-A", status.name);
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.status(t -> t
                .name("Status A-A")
                .translation("nb", l -> l
                        .name("Status A-A nb")
                )
        ).getPublicId();

        Statuses.StatusIndexDocument status = getStatusIndexDocument(id, null);
        assertEquals("Status A-A", status.name);
    }

    @Test
    public void can_add_translation() throws Exception {
        Status statusA = builder.status(t -> t.name("Status A-A"));
        URI id = statusA.getPublicId();

        testUtils.updateResource("/v1/statuses/" + id + "/translations/nb", new StatusTranslations.UpdateStatusTranslationCommand() {{
            name = "Status A-A nb";
        }});

        assertEquals("Status A-A nb", statusA.getTranslation("nb").get().getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        Status status = builder.status(t -> t
                .name("Status A-A")
                .translation("nb", l -> l
                        .name("Status A-A nb")
                )
        );
        URI id = status.getPublicId();

        testUtils.deleteResource("/v1/statuses/" + id + "/translations/nb");

        assertNull(status.getTranslation("nb").orElse(null));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        Status status = builder.status(t -> t
                .name("Status A-A")
                .translation("nb", l -> l.name("Status A-A nb"))
                .translation("en", l -> l.name("Status A-A"))
                .translation("de", l -> l.name("Status A-A de"))
        );
        URI id = status.getPublicId();

        StatusTranslations.StatusTranslationIndexDocument[] translations =
                testUtils.getObject(StatusTranslations.StatusTranslationIndexDocument[].class,
                        testUtils.getResource("/v1/statuses/" + id + "/translations")
                );

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Status A-A nb") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Status A-A") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Status A-A de") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        Status status = builder.status(t -> t
                .name("Status A-A")
                .translation("nb", l -> l.name("Status A-A nb"))
        );
        URI id = status.getPublicId();

        StatusTranslations.StatusTranslationIndexDocument translation = testUtils.getObject(StatusTranslations.StatusTranslationIndexDocument.class,
                testUtils.getResource("/v1/statuses/" + id + "/translations/nb"));
        assertEquals("Status A-A nb", translation.name);
        assertEquals("nb", translation.language);
    }

    private Statuses.StatusIndexDocument getStatusIndexDocument(URI id, String language) throws Exception {
        String path = "/v1/statuses/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return testUtils.getObject(Statuses.StatusIndexDocument.class, testUtils.getResource(path));
    }

}
