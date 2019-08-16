package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TopicFilterTest {
    private Filter filter;
    private Topic topic;
    private Relevance relevance;

    private TopicFilter topicFilter;

    @Before
    public void setUp() {
        filter = mock(Filter.class);
        topic = mock(Topic.class);
        relevance = mock(Relevance.class);

        topicFilter = TopicFilter.create(topic, filter, relevance);
        assertNotNull(topicFilter.getPublicId());
        assertTrue(topicFilter.getPublicId().toString().length() > 4);

        verify(filter).addTopicFilter(topicFilter);
        verify(topic).addTopicFilter(topicFilter);
        verify(relevance).addTopicFilter(topicFilter);
    }

    @Test
    public void getFilter() {
        assertSame(filter, topicFilter.getFilter().orElse(null));
    }

    @Test
    public void getTopic() {
        assertSame(topic, topicFilter.getTopic().orElse(null));
    }

    @Test
    public void getRelevance() {
        assertSame(relevance, topicFilter.getRelevance().orElse(null));
    }

    @Test
    public void preRemove() {
        topicFilter.preRemove();

        verify(filter).removeTopicFilter(topicFilter);
        verify(topic).removeTopicFilter(topicFilter);
        verify(relevance).removeTopicFilter(topicFilter);
    }
}