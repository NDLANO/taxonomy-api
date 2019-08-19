package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TopicResourceTypeTest {
    private Topic topic;
    private ResourceType resourceType;
    private TopicResourceType topicResourceType;

    @Before
    public void setUp() {
        topic = mock(Topic.class);
        resourceType = mock(ResourceType.class);

        topicResourceType = TopicResourceType.create(topic, resourceType);
        assertNotNull(topicResourceType.getPublicId());
        verify(topic).addTopicResourceType(topicResourceType);
        verify(resourceType).addTopicResourceType(topicResourceType);
    }

    @Test
    public void getTopic() {
        assertSame(topic, topicResourceType.getTopic().orElse(null));
    }

    @Test
    public void getResourceType() {
        assertSame(resourceType, topicResourceType.getResourceType().orElse(null));
    }
}