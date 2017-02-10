package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Subject;
import org.junit.Test;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SubjectTranslationsTest extends RestTest {
    @Test
    public void can_get_existing_translation() throws Exception {
        URI id = builder.subject(s -> s
                .name("Mathematics")
                .translation("nb", l -> l
                        .name("Matematikk")
                )
        ).getPublicId();

        Subjects.SubjectIndexDocument subject = getSubject(id, "nb");
        assertEquals("Matematikk", subject.name);
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.subject(s -> s
                .name("Mathematics")
        ).getPublicId();
        Subjects.SubjectIndexDocument subject = getSubject(id, "XX");
        assertEquals("Mathematics", subject.name);
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.subject(s -> s
                .name("Mathematics")
                .translation("nb", l -> l
                        .name("Matematikk")
                )
        ).getPublicId();

        Subjects.SubjectIndexDocument subject = getSubject(id, null);
        assertEquals("Mathematics", subject.name);
    }

    @Test
    public void can_add_translation() throws Exception {
        Subject mathematics = builder.subject(s -> s.name("Mathematics"));
        URI id = mathematics.getPublicId();

        updateResource("/v1/subjects/" + id + "/translations/nb", new SubjectTranslations.UpdateSubjectTranslationCommand() {{
            name = "Matematikk";
        }});

        assertEquals("Matematikk", mathematics.getTranslation("nb").getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("Mathematics")
                .translation("nb", l -> l
                        .name("Matematikk")
                )
        );
        URI id = subject.getPublicId();

        deleteResource("/v1/subjects/" + id + "/translations/nb");

        assertNull(subject.getTranslation("nb"));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("Mathematics")
                .translation("nb", l -> l.name("Matematikk"))
                .translation("en", l -> l.name("Mathematics"))
                .translation("de", l -> l.name("Mathematik"))
        );
        URI id = subject.getPublicId();

        SubjectTranslations.SubjectTranslationIndexDocument[] translations = getObject(SubjectTranslations.SubjectTranslationIndexDocument[].class, getResource("/v1/subjects/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Matematikk") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Mathematics") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Mathematik") && t.language.equals("de"));
    }

    private Subjects.SubjectIndexDocument getSubject(URI id, String language) throws Exception {
        String path = "/v1/subjects/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return getObject(Subjects.SubjectIndexDocument.class, getResource(path));
    }
}
