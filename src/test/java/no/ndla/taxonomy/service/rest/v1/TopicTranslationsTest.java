package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.Topic;
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
        builder.topic(t -> t.name("Mathematics").translation("nb", l -> l.name("Matematikk")));
        builder.topic(t -> t.name("Chemistry").translation("nb", l -> l.name("Kjemi")));

        MockHttpServletResponse response = getResource("/v1/topics?language=nb");
        Topics.TopicIndexDocument[] topics = getObject(Topics.TopicIndexDocument[].class, response);

        assertEquals(2, topics.length);
        assertAnyTrue(topics, s -> s.name.equals("Matematikk"));
        assertAnyTrue(topics, s -> s.name.equals("Kjemi"));
    }

    @Test
    public void can_get_single_topic() throws Exception {
        URI id = builder.topic(t -> t
                .name("Mathematics")
                .translation("nb", l -> l
                        .name("Matematikk")
                )
        ).getPublicId();

        Topics.TopicIndexDocument topic = getTopic(id, "nb");
        assertEquals("Matematikk", topic.name);
    }


    @Test
    public void fallback_to_default_language() throws Exception {
        URI id = builder.topic(t -> t
                .name("Mathematics")
        ).getPublicId();
        Topics.TopicIndexDocument topic = getTopic(id, "XX");
        assertEquals("Mathematics", topic.name);
    }

    @Test
    public void can_get_default_language() throws Exception {
        URI id = builder.topic(t -> t
                .name("Mathematics")
                .translation("nb", l -> l
                        .name("Matematikk")
                )
        ).getPublicId();

        Topics.TopicIndexDocument topic = getTopic(id, null);
        assertEquals("Mathematics", topic.name);
    }

    @Test
    public void can_add_translation() throws Exception {
        Topic mathematics = builder.topic(t -> t.name("Mathematics"));
        URI id = mathematics.getPublicId();

        updateResource("/v1/topics/" + id + "/translations/nb", new TopicTranslations.UpdateTopicTranslationCommand() {{
            name = "Matematikk";
        }});

        assertEquals("Matematikk", mathematics.getTranslation("nb").getName());
    }

    @Test
    public void can_delete_translation() throws Exception {
        Topic topic = builder.topic(t -> t
                .name("Mathematics")
                .translation("nb", l -> l
                        .name("Matematikk")
                )
        );
        URI id = topic.getPublicId();

        deleteResource("/v1/topics/" + id + "/translations/nb");

        assertNull(topic.getTranslation("nb"));
    }

    @Test
    public void can_get_all_translations() throws Exception {
        Topic topic = builder.topic(t -> t
                .name("Mathematics")
                .translation("nb", l -> l.name("Matematikk"))
                .translation("en", l -> l.name("Mathematics"))
                .translation("de", l -> l.name("Mathematik"))
        );
        URI id = topic.getPublicId();

        TopicTranslations.TopicTranslationIndexDocument[] translations = getObject(TopicTranslations.TopicTranslationIndexDocument[].class, getResource("/v1/topics/" + id + "/translations"));

        assertEquals(3, translations.length);
        assertAnyTrue(translations, t -> t.name.equals("Matematikk") && t.language.equals("nb"));
        assertAnyTrue(translations, t -> t.name.equals("Mathematics") && t.language.equals("en"));
        assertAnyTrue(translations, t -> t.name.equals("Mathematik") && t.language.equals("de"));
    }

    @Test
    public void can_get_single_translation() throws Exception {
        Topic topic = builder.topic(t -> t
                .name("Mathematics")
                .translation("nb", l -> l.name("Matematikk"))
        );
        URI id = topic.getPublicId();

        TopicTranslations.TopicTranslationIndexDocument translation = getObject(TopicTranslations.TopicTranslationIndexDocument.class,
                getResource("/v1/topics/" + id + "/translations/nb"));
        assertEquals("Matematikk", translation.name);
        assertEquals("nb", translation.language);
    }

    private Topics.TopicIndexDocument getTopic(URI id, String language) throws Exception {
        String path = "/v1/topics/" + id;
        if (isNotEmpty(language)) path = path + "?language=" + language;
        return getObject(Topics.TopicIndexDocument.class, getResource(path));
    }

}
