package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TopicResourceTypeTest {
    private TopicResourceType topicResourceType;

    @Before
    public void setUp() {
        topicResourceType = new TopicResourceType();
        assertNotNull(topicResourceType.getPublicId());
    }

    @Test
    public void setAndGetTopic() {
        assertFalse(topicResourceType.getTopic().isPresent());

        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);

        topicResourceType.setTopic(topic1);
        verify(topic1).addTopicResourceType(topicResourceType);
        assertSame(topic1, topicResourceType.getTopic().orElse(null));

        clearInvocations(topic1);

        when(topic1.getTopicResourceTypes()).thenReturn(Set.of(topicResourceType));
        topicResourceType.setTopic(topic2);
        verify(topic2).addTopicResourceType(topicResourceType);
        verify(topic1).removeTopicResourceType(topicResourceType);
        assertSame(topic2, topicResourceType.getTopic().orElse(null));

        when(topic2.getTopicResourceTypes()).thenReturn(Set.of(topicResourceType));
        when(topic1.getTopicResourceTypes()).thenReturn(Set.of());

        clearInvocations(topic2);
        topicResourceType.setTopic(null);
        verify(topic2).removeTopicResourceType(topicResourceType);
        assertFalse(topicResourceType.getTopic().isPresent());
    }

    @Test
    public void setAndGetResourceType() {
        assertFalse(topicResourceType.getResourceType().isPresent());

        final var resourceType1 = mock(ResourceType.class);
        final var resourceType2 = mock(ResourceType.class);

        topicResourceType.setResourceType(resourceType1);
        verify(resourceType1).addTopicResourceType(topicResourceType);
        assertSame(resourceType1, topicResourceType.getResourceType().orElse(null));

        clearInvocations(resourceType1);

        when(resourceType1.getTopicResourceTypes()).thenReturn(Set.of(topicResourceType));
        topicResourceType.setResourceType(resourceType2);
        verify(resourceType2).addTopicResourceType(topicResourceType);
        verify(resourceType1).removeTopicResourceType(topicResourceType);
        assertSame(resourceType2, topicResourceType.getResourceType().orElse(null));

        when(resourceType2.getTopicResourceTypes()).thenReturn(Set.of(topicResourceType));
        when(resourceType1.getTopicResourceTypes()).thenReturn(Set.of());

        clearInvocations(resourceType2);
        topicResourceType.setResourceType(null);
        verify(resourceType2).removeTopicResourceType(topicResourceType);
        assertFalse(topicResourceType.getResourceType().isPresent());
    }
}