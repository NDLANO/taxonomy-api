package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TopicResourceTest {
    private Topic topic;
    private Resource resource;
    private TopicResource topicResource;

    @Before
    public void setUp() {
        topic = mock(Topic.class);
        resource = mock(Resource.class);

        topicResource = TopicResource.create(topic, resource);

        verify(topic).addTopicResource(topicResource);
        verify(resource).addTopicResource(topicResource);

        assertNotNull(topicResource.getPublicId());
        assertTrue(topicResource.getPublicId().toString().length() > 4);
    }

    @Test
    public void getTopic() {
        assertSame(topic, topicResource.getTopic().orElse(null));
    }

    @Test
    public void getResource() {
        assertSame(resource, topicResource.getResource().orElse(null));
    }

    @Test
    public void setAndGetRank() {
        assertEquals(0, topicResource.getRank());
        topicResource.setRank(100);
        assertEquals(100, topicResource.getRank());
    }

    @Test
    public void preRemove() {
        topicResource.preRemove();

        assertFalse(topicResource.getResource().isPresent());
        assertFalse(topicResource.getTopic().isPresent());

        verify(topic).removeTopicResource(topicResource);
        verify(resource).removeTopicResource(topicResource);
    }
}