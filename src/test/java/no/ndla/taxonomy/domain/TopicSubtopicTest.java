package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TopicSubtopicTest {
    private TopicSubtopic topicSubtopic;

    @Before
    public void setUp() {
        topicSubtopic = new TopicSubtopic();
        assertNotNull(topicSubtopic.getPublicId());
    }

    @Test
    public void getAndSetTopic() {
        assertFalse(topicSubtopic.getTopic().isPresent());

        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);

        topicSubtopic.setTopic(topic1);
        verify(topic1).addChildTopicSubtopic(topicSubtopic);
        assertSame(topic1, topicSubtopic.getTopic().orElse(null));

        clearInvocations(topic1);

        when(topic1.getChildrenTopicSubtopics()).thenReturn(Set.of(topicSubtopic));
        topicSubtopic.setTopic(topic2);
        verify(topic2).addChildTopicSubtopic(topicSubtopic);
        verify(topic1).removeChildTopicSubTopic(topicSubtopic);
        assertSame(topic2, topicSubtopic.getTopic().orElse(null));

        when(topic2.getChildrenTopicSubtopics()).thenReturn(Set.of(topicSubtopic));
        when(topic1.getChildrenTopicSubtopics()).thenReturn(Set.of());

        clearInvocations(topic2);
        topicSubtopic.setTopic(null);
        verify(topic2).removeChildTopicSubTopic(topicSubtopic);
        assertFalse(topicSubtopic.getTopic().isPresent());
    }

    @Test
    public void getAndSetSubtopic() {
        assertFalse(topicSubtopic.getSubtopic().isPresent());

        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);

        topicSubtopic.setSubtopic(topic1);
        verify(topic1).addParentTopicSubtopic(topicSubtopic);
        assertSame(topic1, topicSubtopic.getSubtopic().orElse(null));
        topicSubtopic.setPrimary(true);

        clearInvocations(topic1);

        when(topic1.getChildrenTopicSubtopics()).thenReturn(Set.of(topicSubtopic));
        topicSubtopic.setSubtopic(topic2);
        verify(topic2).addParentTopicSubtopic(topicSubtopic);
        verify(topic1).removeParentTopicSubtopic(topicSubtopic);
        verify(topic1).setRandomPrimaryTopic();
        assertSame(topic2, topicSubtopic.getSubtopic().orElse(null));

        when(topic2.getChildrenTopicSubtopics()).thenReturn(Set.of(topicSubtopic));
        when(topic1.getChildrenTopicSubtopics()).thenReturn(Set.of());

        clearInvocations(topic2);
        topicSubtopic.setSubtopic(null);
        verify(topic2).removeParentTopicSubtopic(topicSubtopic);
        assertFalse(topicSubtopic.getSubtopic().isPresent());
    }

    @Test
    public void setAndIsPrimary() {
        assertFalse(topicSubtopic.isPrimary());
        topicSubtopic.setPrimary(true);
        assertTrue(topicSubtopic.isPrimary());
        topicSubtopic.setPrimary(false);
        assertFalse(topicSubtopic.isPrimary());
    }

    @Test
    public void getAndSetRank() {
        assertEquals(0, topicSubtopic.getRank());
        topicSubtopic.setRank(10);
        assertEquals(10, topicSubtopic.getRank());
    }

    @Test
    public void preRemove() {
        final var parentTopic = mock(Topic.class);
        final var childTopic = mock(Topic.class);

        topicSubtopic.setTopic(parentTopic);
        topicSubtopic.setSubtopic(childTopic);
        topicSubtopic.setPrimary(true);

        when(parentTopic.getChildrenTopicSubtopics()).thenReturn(Set.of(topicSubtopic));
        when(childTopic.getParentTopicSubtopics()).thenReturn(Set.of(topicSubtopic));

        assertTrue(topicSubtopic.getTopic().isPresent());
        assertTrue(topicSubtopic.getSubtopic().isPresent());

        clearInvocations(parentTopic, childTopic);

        topicSubtopic.preRemove();

        verify(childTopic).setRandomPrimaryTopic();
        verify(parentTopic).removeChildTopicSubTopic(topicSubtopic);
        verify(childTopic).removeParentTopicSubtopic(topicSubtopic);
        assertFalse(topicSubtopic.getTopic().isPresent());
        assertFalse(topicSubtopic.getSubtopic().isPresent());
    }
}