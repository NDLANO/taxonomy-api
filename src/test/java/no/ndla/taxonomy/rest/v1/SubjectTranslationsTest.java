/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.dtos.EntityWithPathDTO;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.assertAllTrue;
import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SubjectTranslationsTest extends RestTest {
    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_all_subjects() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.name("Mathematics").translation("nb", l -> l.name("Matematikk")));
        builder.node(NodeType.SUBJECT, s -> s.name("Chemistry").translation("nb", l -> l.name("Kjemi")));

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects?language=nb");
        NodeDTO[] subjects = testUtils.getObject(NodeDTO[].class, response);

        assertEquals(2, subjects.length);
        assertAnyTrue(subjects, s -> s.getName().equals("Matematikk"));
        assertAnyTrue(subjects, s -> s.getName().equals("Kjemi"));
    }

    @Test
    public void can_get_single_subject() throws Exception {
        URI id = builder.node(NodeType.SUBJECT, s -> s.name("Mathematics").translation("nb", l -> l.name("Matematikk")))
                .getPublicId();

        EntityWithPathDTO subject = getSubject(id, "nb");
        assertEquals("Matematikk", subject.getName());
    }

    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.node(NodeType.SUBJECT, s -> s.name("Mathematics")).getPublicId();
        EntityWithPathDTO subject = getSubject(id, "XX");
        assertEquals("Mathematics", subject.getName());
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.node(NodeType.SUBJECT, s -> s.name("Mathematics").translation("nb", l -> l.name("Matematikk")))
                .getPublicId();

        EntityWithPathDTO subject = getSubject(id, null);
        assertEquals("Mathematics", subject.getName());
    }

    @Test
    public void can_add_translation() throws Exception {
        Node mathematics = builder.node(NodeType.SUBJECT, s -> s.name("Mathematics"));
        URI id = mathematics.getPublicId();

        testUtils.updateResource("/v1/subjects/" + id + "/translations/nb",
                new SubjectTranslations.UpdateSubjectTranslationCommand() {
                    {
                        name = "Matematikk";
                    }
                });

        assertEquals("Matematikk", mathematics.getTranslation("nb").get().getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT,
                s -> s.name("Mathematics").translation("nb", l -> l.name("Matematikk")));
        URI id = subject.getPublicId();

        testUtils.deleteResource("/v1/subjects/" + id + "/translations/nb");

        assertNull(subject.getTranslation("nb").orElse(null));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT,
                s -> s.name("Mathematics").translation("nb", l -> l.name("Matematikk"))
                        .translation("en", l -> l.name("Mathematics")).translation("de", l -> l.name("Mathematik")));
        URI id = subject.getPublicId();

        var translations = testUtils.getObject(SubjectTranslations.SubjectTranslationIndexDocument[].class,
                testUtils.getResource("/v1/subjects/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Matematikk") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Mathematics") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Mathematik") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT,
                s -> s.name("Mathematics").translation("nb", l -> l.name("Matematikk")));
        URI id = subject.getPublicId();

        var translation = testUtils.getObject(SubjectTranslations.SubjectTranslationIndexDocument.class,
                testUtils.getResource("/v1/subjects/" + id + "/translations/nb"));
        assertEquals("Matematikk", translation.name);
        assertEquals("nb", translation.language);
    }

    @Test
    public void can_get_topics_with_language() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT,
                s -> s.name("physics")
                        .child(NodeType.TOPIC, t -> t.name("statics").translation("nb", tr -> tr.name("statikk")))
                        .child(NodeType.TOPIC,
                                t -> t.name("electricity").translation("nb", tr -> tr.name("elektrisitet")))
                        .child(NodeType.TOPIC, t -> t.name("optics").translation("nb", tr -> tr.name("optikk"))));

        var response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics?language=nb");
        var topics = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(topics, t -> "statikk".equals(t.getName()));
        assertAnyTrue(topics, t -> "elektrisitet".equals(t.getName()));
        assertAnyTrue(topics, t -> "optikk".equals(t.getName()));
    }

    @Test
    public void can_get_translated_resources() throws Exception {

        builder.resourceType("article", rt -> rt.name("Article").translation("nb", tr -> tr.name("Artikkel")));

        URI id = builder.node(NodeType.SUBJECT, s -> s.name("subject")
                .child(NodeType.TOPIC, t -> t.name("Trigonometry").resource(r -> r.name("Introduction to trigonometry")
                        .translation("nb", tr -> tr.name("Introduksjon til trigonometri")).resourceType("article")))
                .child(NodeType.TOPIC, t -> t.name("Calculus").resource(r -> r.name("Introduction to calculus")
                        .translation("nb", tr -> tr.name("Introduksjon til calculus")).resourceType("article"))))
                .getPublicId();

        var response = testUtils.getResource("/v1/subjects/" + id + "/resources?language=nb");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals(2, resources.length);

        assertAnyTrue(resources, r -> r.getName().equals("Introduksjon til trigonometri"));
        assertAnyTrue(resources, r -> r.getName().equals("Introduksjon til calculus"));
        assertAllTrue(resources, r -> r.getResourceTypes().iterator().next().getName().equals("Artikkel"));
    }

    private NodeDTO getSubject(URI id, String language) throws Exception {
        String path = "/v1/subjects/" + id;
        if (isNotEmpty(language))
            path = path + "?language=" + language;
        return testUtils.getObject(NodeDTO.class, testUtils.getResource(path));
    }
}
