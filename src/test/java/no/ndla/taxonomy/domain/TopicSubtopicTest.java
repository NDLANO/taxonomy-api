package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TopicSubtopicTest {
    private Topic topic;
    private Topic subTopic;

    private TopicSubtopic topicSubtopic;

    @Before
    public void setUp() {
        topic = mock(Topic.class);
        subTopic = mock(Topic.class);

        topicSubtopic = TopicSubtopic.create(topic, subTopic);
        assertNotNull(topicSubtopic.getPublicId());
    }

    @Test
    public void getTopic() {
        assertSame(topic, topicSubtopic.getTopic().orElse(null));
    }

    @Test
    public void getSubtopic() {
        assertSame(subTopic, topicSubtopic.getSubtopic().orElse(null));
    }

    @Test
    public void getAndSetRank() {
        assertEquals(0, topicSubtopic.getRank());
        topicSubtopic.setRank(10);
        assertEquals(10, topicSubtopic.getRank());
    }

    @Test
    public void preRemove() {
        topicSubtopic.preRemove();

        verify(topic).removeChildTopicSubTopic(topicSubtopic);
        verify(subTopic).removeParentTopicSubtopic(topicSubtopic);
        assertFalse(topicSubtopic.getTopic().isPresent());
        assertFalse(topicSubtopic.getSubtopic().isPresent());
    }
}