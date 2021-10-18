/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class TopicTest {
    private Topic topic;

    @BeforeEach
    public void setUp() {
        topic = new Topic();
    }

    @Test
    public void name() {
        assertEquals(topic, topic.name("test name 1"));
        assertEquals("test name 1", topic.getName());
    }

    @Test
    public void getPrimaryPath() {
        assertFalse(topic.getPrimaryPath().isPresent());

        final var cachedPrimaryContextUrl = mock(CachedPath.class);
        final var cachedPrimarySubjectUrl = mock(CachedPath.class);

        when(cachedPrimaryContextUrl.getPath()).thenReturn("/topic/primary");
        when(cachedPrimarySubjectUrl.getPath()).thenReturn("/subject/primary");

        // If returning a primary subject path along it must return it
        setField(topic, "cachedPaths", Set.of(cachedPrimarySubjectUrl));
        assertEquals("/subject/primary", topic.getPrimaryPath().orElse(""));

        // And adding a primary context URL (topic) it must be returned instead
        setField(topic, "cachedPaths", Set.of(cachedPrimarySubjectUrl, cachedPrimaryContextUrl));
        assertEquals("/topic/primary", topic.getPrimaryPath().orElse(""));

        // Order must not matter
        setField(topic, "cachedPaths", Set.of(cachedPrimaryContextUrl, cachedPrimarySubjectUrl));
        assertEquals("/topic/primary", topic.getPrimaryPath().orElse(""));
    }

    @Test
    public void getAddAndRemoveSubjectTopics() {
        final var topic = spy(this.topic);

        assertEquals(0, topic.getSubjectTopics().size());

        final var subjectTopic1 = mock(SubjectTopic.class);
        final var subjectTopic2 = mock(SubjectTopic.class);

        try {
            topic.addSubjectTopic(subjectTopic1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }

        when(subjectTopic1.getTopic()).thenReturn(Optional.of(topic));
        topic.addSubjectTopic(subjectTopic1);

        when(subjectTopic1.getTopic()).thenReturn(Optional.of(topic));

        assertEquals(1, topic.getSubjectTopics().size());
        assertTrue(topic.getSubjectTopics().contains(subjectTopic1));

        try {
            topic.addSubjectTopic(subjectTopic2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(subjectTopic2.getTopic()).thenReturn(Optional.of(topic));
        topic.addSubjectTopic(subjectTopic2);

        when(subjectTopic2.getTopic()).thenReturn(Optional.of(topic));

        assertEquals(2, topic.getSubjectTopics().size());
        assertTrue(topic.getSubjectTopics().containsAll(Set.of(subjectTopic1, subjectTopic2)));

        topic.removeSubjectTopic(subjectTopic1);
        assertEquals(1, topic.getSubjectTopics().size());
        assertTrue(topic.getSubjectTopics().contains(subjectTopic2));
        verify(subjectTopic1).disassociate();

        topic.removeSubjectTopic(subjectTopic2);
        assertEquals(0, topic.getSubjectTopics().size());
        verify(subjectTopic2).disassociate();
    }

    @Test
    public void addGetAndRemoveChildrenTopicSubtopics() {
        assertEquals(0, topic.getChildrenTopicSubtopics().size());

        final var topicSubtopic1 = mock(TopicSubtopic.class);
        final var topicSubtopic2 = mock(TopicSubtopic.class);

        try {
            topic.addChildTopicSubtopic(topicSubtopic1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {

        }
        when(topicSubtopic1.getTopic()).thenReturn(Optional.of(topic));
        topic.addChildTopicSubtopic(topicSubtopic1);

        assertEquals(1, topic.getChildrenTopicSubtopics().size());
        assertTrue(topic.getChildrenTopicSubtopics().contains(topicSubtopic1));

        try {
            topic.addChildTopicSubtopic(topicSubtopic2);
        } catch (IllegalArgumentException ignored) {
        }
        when(topicSubtopic2.getTopic()).thenReturn(Optional.of(topic));
        topic.addChildTopicSubtopic(topicSubtopic2);

        assertEquals(2, topic.getChildrenTopicSubtopics().size());
        assertTrue(topic.getChildrenTopicSubtopics().containsAll(Set.of(topicSubtopic1, topicSubtopic2)));

        topic.removeChildTopicSubTopic(topicSubtopic1);
        verify(topicSubtopic1).disassociate();
        assertEquals(1, topic.getChildrenTopicSubtopics().size());
        assertTrue(topic.getChildrenTopicSubtopics().contains(topicSubtopic2));

        topic.removeChildTopicSubTopic(topicSubtopic2);
        verify(topicSubtopic2).disassociate();
        assertEquals(0, topic.getChildrenTopicSubtopics().size());
    }

    @Test
    public void addGetAndRemoveParentTopicSubtopics() {
        assertFalse(topic.getParentTopicSubtopic().isPresent());

        final var topicSubtopic1 = mock(TopicSubtopic.class);
        final var topicSubtopic2 = mock(TopicSubtopic.class);

        try {
            topic.addParentTopicSubtopic(topicSubtopic1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicSubtopic1.getSubtopic()).thenReturn(Optional.of(topic));
        topic.addParentTopicSubtopic(topicSubtopic1);

        assertTrue(topic.getParentTopicSubtopic().isPresent());
        assertSame(topicSubtopic1, topic.getParentTopicSubtopic().orElseThrow());

        try {
            topic.addParentTopicSubtopic(topicSubtopic2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        topic.removeParentTopicSubtopic(topicSubtopic1);
        verify(topicSubtopic1).disassociate();
        assertFalse(topic.getParentTopicSubtopic().isPresent());
    }

    @Test
    public void addGetAndRemoveTopicResources() {
        assertEquals(0, topic.getTopicResources().size());

        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);

        try {
            topic.addTopicResource(topicResource1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }

        when(topicResource1.getTopic()).thenReturn(Optional.of(topic));
        topic.addTopicResource(topicResource1);

        assertEquals(1, topic.getTopicResources().size());
        assertTrue(topic.getTopicResources().contains(topicResource1));

        try {
            topic.addTopicResource(topicResource2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicResource2.getTopic()).thenReturn(Optional.of(topic));
        topic.addTopicResource(topicResource2);

        assertEquals(2, topic.getTopicResources().size());
        assertTrue(topic.getTopicResources().containsAll(Set.of(topicResource1, topicResource2)));

        topic.removeTopicResource(topicResource1);
        verify(topicResource1).disassociate();
        assertEquals(1, topic.getTopicResources().size());
        assertTrue(topic.getTopicResources().contains(topicResource2));

        topic.removeTopicResource(topicResource2);
        verify(topicResource2).disassociate();
        assertEquals(0, topic.getTopicResources().size());
    }

    @Test
    public void getParentTopic() {
        final var parentTopic1 = mock(Topic.class);

        final var topicSubtopic1 = mock(TopicSubtopic.class);

        when(topicSubtopic1.getSubtopic()).thenReturn(Optional.of(topic));
        when(topicSubtopic1.getTopic()).thenReturn(Optional.of(parentTopic1));

        assertFalse(topic.getParentTopic().isPresent());

        topic.addParentTopicSubtopic(topicSubtopic1);

        assertTrue(topic.getParentTopic().isPresent());
        assertSame(parentTopic1, topic.getParentTopic().orElseThrow());
    }

    @Test
    public void getResources() {
        final var resource1 = mock(Resource.class);
        final var resource2 = mock(Resource.class);

        final var topicResource1 = mock(TopicResource.class);
        final var topicResource2 = mock(TopicResource.class);
        final var topicResource3 = mock(TopicResource.class);

        Set.of(topicResource1, topicResource2, topicResource3)
                .forEach(topicResource -> when(topicResource.getTopic()).thenReturn(Optional.of(topic)));
        when(topicResource1.getResource()).thenReturn(Optional.of(resource1));
        when(topicResource2.getResource()).thenReturn(Optional.of(resource2));

        topic.addTopicResource(topicResource1);
        topic.addTopicResource(topicResource2);
        topic.addTopicResource(topicResource3);

        assertEquals(3, topic.getTopicResources().size());
        assertEquals(2, topic.getResources().size());
        assertTrue(topic.getResources().containsAll(Set.of(resource1, resource2)));
    }

    @Test
    public void setAndGetContentUri() throws URISyntaxException {
        assertNull(topic.getContentUri());
        topic.setContentUri(new URI("urn:test1"));
        assertEquals("urn:test1", topic.getContentUri().toString());
    }

    @Test
    public void setAndIsContext() {
        assertFalse(topic.isContext());
        topic.setContext(true);
        assertTrue(topic.isContext());
        topic.setContext(false);
        assertFalse(topic.isContext());
    }

    @Test
    public void addAndGetAndRemoveTranslation() {
        assertEquals(0, topic.getTranslations().size());

        var returnedTranslation = topic.addTranslation("nb");
        assertEquals(1, topic.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(topic.getTranslations().contains(returnedTranslation));
        assertEquals(topic, returnedTranslation.getTopic());

        var returnedTranslation2 = topic.addTranslation("en");
        assertEquals(2, topic.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(topic.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(topic, returnedTranslation2.getTopic());

        topic.removeTranslation("nb");

        assertNull(returnedTranslation.getTopic());
        assertFalse(topic.getTranslations().contains(returnedTranslation));

        assertFalse(topic.getTranslation("nb").isPresent());

        topic.addTranslation(returnedTranslation);
        assertEquals(topic, returnedTranslation.getTopic());
        assertTrue(topic.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, topic.getTranslation("nb").get());
        assertEquals(returnedTranslation2, topic.getTranslation("en").orElse(null));
    }

    @Test
    public void preRemove() {
        final var parentTopicSubtopics = Set.of(mock(TopicSubtopic.class), mock(TopicSubtopic.class));
        final var childTopicSubtopics = Set.of(mock(TopicSubtopic.class), mock(TopicSubtopic.class));
        final var subjectTopics = Set.of(mock(SubjectTopic.class), mock(SubjectTopic.class));
        final var topicResources = Set.of(mock(TopicResource.class), mock(TopicResource.class));

        parentTopicSubtopics.forEach(topicSubtopic -> {
            when(topicSubtopic.getSubtopic()).thenReturn(Optional.of(topic));
            topic.addParentTopicSubtopic(topicSubtopic);
        });

        childTopicSubtopics.forEach(topicSubtopic -> {
            when(topicSubtopic.getTopic()).thenReturn(Optional.of(topic));
            topic.addChildTopicSubtopic(topicSubtopic);
        });

        subjectTopics.forEach(subjectTopic -> {
            when(subjectTopic.getTopic()).thenReturn(Optional.of(topic));
            topic.addSubjectTopic(subjectTopic);
        });

        topicResources.forEach(topicResource -> {
            when(topicResource.getTopic()).thenReturn(Optional.of(topic));
            topic.addTopicResource(topicResource);
        });

        topic.preRemove();

        parentTopicSubtopics.forEach(topicSubtopic -> verify(topicSubtopic).disassociate());
        childTopicSubtopics.forEach(topicSubtopic -> verify(topicSubtopic).disassociate());
        subjectTopics.forEach(subjectTopic -> verify(subjectTopic).disassociate());
        topicResources.forEach(topicResource -> verify(topicResource).disassociate());
    }

    @Test
    public void getCachedPaths() {
        final var cachedPaths = Set.of();

        setField(topic, "cachedPaths", cachedPaths);
        assertSame(cachedPaths, topic.getCachedPaths());
    }
}
