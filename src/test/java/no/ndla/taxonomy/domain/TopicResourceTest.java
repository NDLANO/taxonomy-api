package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TopicResourceTest {
    private TopicResource topicResource;

    @Before
    public void setUp() {
        topicResource = new TopicResource();
        assertNotNull(topicResource.getPublicId());
        assertTrue(topicResource.getPublicId().toString().length() > 4);
    }

    @Test
    public void setAndGetTopic() {
        assertFalse(topicResource.getTopic().isPresent());

        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);

        topicResource.setTopic(topic1);
        verify(topic1).addTopicResource(topicResource);
        assertSame(topic1, topicResource.getTopic().orElse(null));

        clearInvocations(topic1);

        when(topic1.getTopicResources()).thenReturn(Set.of(topicResource));
        topicResource.setTopic(topic2);
        verify(topic2).addTopicResource(topicResource);
        verify(topic1).removeTopicResource(topicResource);
        assertSame(topic2, topicResource.getTopic().orElse(null));

        when(topic2.getTopicResources()).thenReturn(Set.of(topicResource));
        when(topic1.getTopicResources()).thenReturn(Set.of());

        clearInvocations(topic2);
        topicResource.setTopic(null);
        verify(topic2).removeTopicResource(topicResource);
        assertFalse(topicResource.getTopic().isPresent());
    }

    @Test
    public void setAndGetResource() {
        assertFalse(topicResource.getTopic().isPresent());

        final var resource1 = mock(Resource.class);
        final var resource2 = mock(Resource.class);

        topicResource.setResource(resource1);
        verify(resource1).addTopicResource(topicResource);
        assertSame(resource1, topicResource.getResource().orElse(null));
        topicResource.setPrimary(true);

        clearInvocations(resource1);

        when(resource1.getTopicResources()).thenReturn(Set.of(topicResource));
        topicResource.setResource(resource2);
        verify(resource2).addTopicResource(topicResource);
        verify(resource1).removeTopicResource(topicResource);
        verify(resource1).setRandomPrimaryTopic();
        assertSame(resource2, topicResource.getResource().orElse(null));

        when(resource2.getTopicResources()).thenReturn(Set.of(topicResource));
        when(resource1.getTopicResources()).thenReturn(Set.of());

        clearInvocations(resource2);
        topicResource.setResource(null);
        verify(resource2).removeTopicResource(topicResource);
        assertFalse(topicResource.getResource().isPresent());
    }

    @Test
    public void setAndIsPrimary() {
        assertFalse(topicResource.isPrimary());
        topicResource.setPrimary(true);
        assertTrue(topicResource.isPrimary());
        topicResource.setPrimary(false);
        assertFalse(topicResource.isPrimary());
    }

    @Test
    public void setAndGetRank() {
        assertEquals(0, topicResource.getRank());
        topicResource.setRank(100);
        assertEquals(100, topicResource.getRank());
    }

    @Test
    public void testToString() throws URISyntaxException {
        final var topic = mock(Topic.class);
        final var resource = mock(Resource.class);

        when(topic.getName()).thenReturn("topic-name");
        when(resource.getName()).thenReturn("resource-name");
        when(topic.getPublicId()).thenReturn(new URI("urn:topic-id1"));
        when(resource.getPublicId()).thenReturn(new URI("urn:resource-id1"));

        topicResource.setTopic(topic);
        topicResource.setResource(resource);
        topicResource.setRank(100);
        topicResource.setPrimary(true);

        assertEquals("TopicResource: { topic-name urn:topic-id1 -> resource-name urn:resource-id1 P 100}", topicResource.toString());

        topicResource.setRank(101);
        topicResource.setPrimary(false);

        assertEquals("TopicResource: { topic-name urn:topic-id1 -> resource-name urn:resource-id1  101}", topicResource.toString());
    }

    @Test
    public void preRemove() {
        final var topic = mock(Topic.class);
        final var resource = mock(Resource.class);

        topicResource.setTopic(topic);
        topicResource.setResource(resource);

        assertTrue(topicResource.getResource().isPresent());
        assertTrue(topicResource.getTopic().isPresent());

        when(topic.getTopicResources()).thenReturn(Set.of(topicResource));
        when(resource.getTopicResources()).thenReturn(Set.of(topicResource));

        topicResource.preRemove();

        assertFalse(topicResource.getResource().isPresent());
        assertFalse(topicResource.getTopic().isPresent());

        verify(topic).removeTopicResource(topicResource);
        verify(resource).removeTopicResource(topicResource);
    }
}