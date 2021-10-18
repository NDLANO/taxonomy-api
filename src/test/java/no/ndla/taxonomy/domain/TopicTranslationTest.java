/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class TopicTranslationTest {
    private TopicTranslation topicTranslation;

    @BeforeEach
    public void setUp() {
        topicTranslation = new TopicTranslation();
    }

    @Test
    public void testConstructor() {
        var topic = mock(Topic.class);
        var topicTranslation2 = new TopicTranslation(topic, "en");
        assertEquals("en", topicTranslation2.getLanguageCode());
        assertEquals(topic, topicTranslation2.getTopic());
        verify(topic).addTranslation(topicTranslation2);
    }

    @Test
    public void getAndSetTopic() {
        var topic1 = mock(Topic.class);
        var topic2 = mock(Topic.class);

        when(topic1.getTranslations()).thenReturn(Set.of());
        when(topic2.getTranslations()).thenReturn(Set.of());

        topicTranslation.setTopic(topic1);
        verify(topic1).addTranslation(topicTranslation);

        when(topic1.getTranslations()).thenReturn(Set.of(topicTranslation));

        topicTranslation.setTopic(topic2);
        verify(topic2).addTranslation(topicTranslation);
        verify(topic1).removeTranslation(topicTranslation);
    }

    @Test
    public void setAndGetName() {
        assertNull(topicTranslation.getName());

        topicTranslation.setName("test1");
        assertEquals("test1", topicTranslation.getName());
    }

    @Test
    public void getLanguageCode() {
        setField(topicTranslation, "languageCode", "nb");
        assertEquals("nb", topicTranslation.getLanguageCode());
    }
}
