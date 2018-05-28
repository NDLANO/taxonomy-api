package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Subject;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SubjectTranslationsTest extends RestTest {
    @Test
    public void can_get_all_subjects() throws Exception {
        builder.subject(s -> s.name("Mathematics").translation("nb", l -> l.name("Matematikk")));
        builder.subject(s -> s.name("Chemistry").translation("nb", l -> l.name("Kjemi")));

        MockHttpServletResponse response = getResource("/v1/subjects?language=nb");
        Subjects.SubjectIndexDocument[] subjects = getObject(Subjects.SubjectIndexDocument[].class, response);

        assertEquals(2, subjects.length);
        assertAnyTrue(subjects, s -> s.name.equals("Matematikk"));
        assertAnyTrue(subjects, s -> s.name.equals("Kjemi"));
    }

    @Test
    public void can_get_single_subject() throws Exception {
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

    @Test
    public void can_get_single_translation() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("Mathematics")
                .translation("nb", l -> l.name("Matematikk"))
        );
        URI id = subject.getPublicId();

        SubjectTranslations.SubjectTranslationIndexDocument translation = getObject(SubjectTranslations.SubjectTranslationIndexDocument.class,
                getResource("/v1/subjects/" + id + "/translations/nb"));
        assertEquals("Matematikk", translation.name);
        assertEquals("nb", translation.language);
    }

    @Test
    public void can_get_topics_with_language() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("physics")
                .topic(t -> t.name("statics").translation("nb", tr -> tr.name("statikk")))
                .topic(t -> t.name("electricity").translation("nb", tr -> tr.name("elektrisitet")))
                .topic(t -> t.name("optics").translation("nb", tr -> tr.name("optikk")))
        );

        MockHttpServletResponse response = getResource("/v1/subjects/" + subject.getPublicId() + "/topics?language=nb");
        Subjects.SubTopicIndexDocument[] topics = getObject(Subjects.SubTopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(topics, t -> "statikk".equals(t.name));
        assertAnyTrue(topics, t -> "elektrisitet".equals(t.name));
        assertAnyTrue(topics, t -> "optikk".equals(t.name));
    }

    @Test
    public void can_get_translated_resources() throws Exception {

        builder.resourceType("article", rt -> rt
                .name("Article")
                .translation("nb", tr -> tr.name("Artikkel"))
        );

        URI id = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("Trigonometry")
                        .resource(r -> r
                                .name("Introduction to trigonometry")
                                .translation("nb", tr -> tr.name("Introduksjon til trigonometri"))
                                .resourceType("article")
                        )
                )
                .topic(t -> t
                        .name("Calculus")
                        .resource(r -> r
                                .name("Introduction to calculus")
                                .translation("nb", tr -> tr.name("Introduksjon til calculus"))
                                .resourceType("article")
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + id + "/resources?language=nb");
        Subjects.ResourceIndexDocument[] resources = getObject(Subjects.ResourceIndexDocument[].class, response);

        assertEquals(2, resources.length);

        assertAnyTrue(resources, r -> r.name.equals("Introduksjon til trigonometri"));
        assertAnyTrue(resources, r -> r.name.equals("Introduksjon til calculus"));
        assertAllTrue(resources, r -> r.resourceTypes.iterator().next().name.equals("Artikkel"));
    }

    private Subjects.SubjectIndexDocument getSubject(URI id, String language) throws Exception {
        String path = "/v1/subjects/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return getObject(Subjects.SubjectIndexDocument.class, getResource(path));
    }
}
