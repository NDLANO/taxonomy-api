package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Topic;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TopicTranslationsTest extends RestTest {

    @Test
    public void can_get_all_topics() throws Exception {
        builder.topic(t -> t.name("Trigonometry").translation("nb", l -> l.name("Trigonometri")));
        builder.topic(t -> t.name("Integration").translation("nb", l -> l.name("Integrasjon")));

        MockHttpServletResponse response = getResource("/v1/topics?language=nb");
        Topics.TopicIndexDocument[] topics = getObject(Topics.TopicIndexDocument[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, s -> s.name.equals("Trigonometri"));
        assertAnyTrue(topics, s -> s.name.equals("Integrasjon"));
    }

    @Test
    public void can_get_single_topic() throws Exception {
        URI id = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l
                        .name("Trigonometri")
                )
        ).getPublicId();

        Topics.TopicIndexDocument topic = getTopic(id, "nb");
        assertEquals("Trigonometri", topic.name);
    }


    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.topic(t -> t
                .name("Trigonometry")
        ).getPublicId();
        Topics.TopicIndexDocument topic = getTopic(id, "XX");
        assertEquals("Trigonometry", topic.name);
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l
                        .name("Trigonometri")
                )
        ).getPublicId();

        Topics.TopicIndexDocument topic = getTopic(id, null);
        assertEquals("Trigonometry", topic.name);
    }

    @Test
    public void can_add_translation() throws Exception {
        Topic trigonometry = builder.topic(t -> t.name("Trigonometry"));
        URI id = trigonometry.getPublicId();

        updateResource("/v1/topics/" + id + "/translations/nb", new TopicTranslations.UpdateTopicTranslationCommand() {{
            name = "Trigonometri";
        }});

        assertEquals("Trigonometri", trigonometry.getTranslation("nb").getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        Topic topic = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l
                        .name("Trigonometri")
                )
        );
        URI id = topic.getPublicId();

        deleteResource("/v1/topics/" + id + "/translations/nb");

        assertNull(topic.getTranslation("nb"));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        Topic topic = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l.name("Trigonometri"))
                .translation("en", l -> l.name("Trigonometry"))
                .translation("de", l -> l.name("Trigonometrie"))
        );
        URI id = topic.getPublicId();

        TopicTranslations.TopicTranslationIndexDocument[] translations = getObject(TopicTranslations.TopicTranslationIndexDocument[].class, getResource("/v1/topics/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Trigonometri") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Trigonometry") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Trigonometrie") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        Topic topic = builder.topic(t -> t
                .name("Trigonometry")
                .translation("nb", l -> l.name("Trigonometri"))
        );
        URI id = topic.getPublicId();

        TopicTranslations.TopicTranslationIndexDocument translation = getObject(TopicTranslations.TopicTranslationIndexDocument.class,
                getResource("/v1/topics/" + id + "/translations/nb"));
        assertEquals("Trigonometri", translation.name);
        assertEquals("nb", translation.language);
    }



    @Test
    public void can_get_resources_for_a_topic_recursively_with_translation() throws Exception {
        builder.resourceType("article", rt -> rt.name("Article").translation("nb", tr -> tr.name("Artikkel")));

        URI a = builder.topic(t -> t
                .resource(r -> r
                        .name("Introduction to calculus")
                        .translation("nb", tr -> tr.name("Introduksjon til calculus"))
                        .resourceType("article")
                )
                .subtopic(st -> st
                        .resource(r -> r
                                .name("Introduction to integration")
                                .translation("nb", tr -> tr.name("Introduksjon til integrasjon"))
                                .resourceType("article")
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/topics/" + a + "/resources?recursive=true&language=nb");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "Introduksjon til calculus".equals(r.name));
        assertAnyTrue(result, r -> "Introduksjon til integrasjon".equals(r.name));
        assertAllTrue(result, r -> r.resourceTypes.iterator().next().name.equals("Artikkel"));
    }

    @Test
    public void can_get_resources_for_a_topic_without_child_topic_resources_with_translation() throws Exception {
        builder.resourceType("article", rt -> rt.name("Article").translation("nb", tr -> tr.name("Artikkel")));

        builder.subject(s -> s
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r
                                .name("resource 1")
                                .translation("nb", tr -> tr.name("ressurs 1"))
                                .resourceType("article")
                        )
                        .resource(r -> r
                                .name("resource 2")
                                .translation("nb", tr -> tr.name("ressurs 2"))
                                .resourceType("article")
                        )
                        .subtopic(st -> st.name("subtopic").resource(r -> r.name("subtopic resource")))
                ));

        MockHttpServletResponse response = getResource("/v1/topics/urn:topic:1/resources?language=nb");
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "ressurs 1".equals(r.name));
        assertAnyTrue(result, r -> "ressurs 2".equals(r.name));
        assertAllTrue(result, r -> r.resourceTypes.iterator().next().name.equals("Artikkel"));
    }

    private Topics.TopicIndexDocument getTopic(URI id, String language) throws Exception {
        String path = "/v1/topics/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return getObject(Topics.TopicIndexDocument.class, getResource(path));
    }

}
