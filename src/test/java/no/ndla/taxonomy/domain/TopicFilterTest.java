package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TopicFilterTest {
    private TopicFilter topicFilter;

    @Before
    public void setUp() {
        topicFilter = new TopicFilter();
        assertNotNull(topicFilter.getPublicId());
        assertTrue(topicFilter.getPublicId().toString().length() > 4);
    }

    @Test
    public void getAndSetFilter() {
        assertFalse(topicFilter.getFilter().isPresent());

        final var filter1 = mock(Filter.class);
        final var filter2 = mock(Filter.class);

        topicFilter.setFilter(filter1);
        verify(filter1).addTopicFilter(topicFilter);
        assertSame(filter1, topicFilter.getFilter().orElse(null));

        clearInvocations(filter1);

        when(filter1.getTopicFilters()).thenReturn(Set.of(topicFilter));
        topicFilter.setFilter(filter2);
        verify(filter2).addTopicFilter(topicFilter);
        verify(filter1).removeTopicFilter(topicFilter);
        assertSame(filter2, topicFilter.getFilter().orElse(null));

        when(filter2.getTopicFilters()).thenReturn(Set.of(topicFilter));
        when(filter1.getTopicFilters()).thenReturn(Set.of());

        clearInvocations(filter2);
        topicFilter.setFilter(null);
        verify(filter2).removeTopicFilter(topicFilter);
        assertFalse(topicFilter.getFilter().isPresent());
    }

    @Test
    public void getAndSetTopic() {
        assertFalse(topicFilter.getTopic().isPresent());

        final var topic1 = mock(Topic.class);
        final var topic2 = mock(Topic.class);

        topicFilter.setTopic(topic1);
        verify(topic1).addTopicFilter(topicFilter);
        assertSame(topic1, topicFilter.getTopic().orElse(null));

        clearInvocations(topic1);

        when(topic1.getTopicFilters()).thenReturn(Set.of(topicFilter));
        topicFilter.setTopic(topic2);
        verify(topic2).addTopicFilter(topicFilter);
        verify(topic1).removeTopicFilter(topicFilter);
        assertSame(topic2, topicFilter.getTopic().orElse(null));

        when(topic2.getTopicFilters()).thenReturn(Set.of(topicFilter));
        when(topic1.getTopicFilters()).thenReturn(Set.of());

        clearInvocations(topic2);
        topicFilter.setTopic(null);
        verify(topic2).removeTopicFilter(topicFilter);
        assertFalse(topicFilter.getTopic().isPresent());
    }

    @Test
    public void getAndSetRelevance() {
        assertFalse(topicFilter.getTopic().isPresent());

        final var relevance1 = mock(Relevance.class);
        final var relevance2 = mock(Relevance.class);

        topicFilter.setRelevance(relevance1);
        verify(relevance1).addTopicFilter(topicFilter);
        assertSame(relevance1, topicFilter.getRelevance().orElse(null));

        clearInvocations(relevance1);

        when(relevance1.getTopicFilters()).thenReturn(Set.of(topicFilter));
        topicFilter.setRelevance(relevance2);
        verify(relevance2).addTopicFilter(topicFilter);
        verify(relevance1).removeTopicFilter(topicFilter);
        assertSame(relevance2, topicFilter.getRelevance().orElse(null));

        when(relevance2.getTopicFilters()).thenReturn(Set.of(topicFilter));
        when(relevance1.getTopicFilters()).thenReturn(Set.of());

        clearInvocations(relevance2);
        topicFilter.setRelevance(null);
        verify(relevance2).removeTopicFilter(topicFilter);
        assertFalse(topicFilter.getRelevance().isPresent());
    }

    @Test
    public void preRemove() {
        final var relevance = mock(Relevance.class);
        final var topic = mock(Topic.class);
        final var filter = mock(Filter.class);

        topicFilter.setRelevance(relevance);
        topicFilter.setTopic(topic);
        topicFilter.setFilter(filter);

        when(relevance.getTopicFilters()).thenReturn(Set.of(topicFilter));
        when(topic.getTopicFilters()).thenReturn(Set.of(topicFilter));
        when(filter.getTopicFilters()).thenReturn(Set.of(topicFilter));

        clearInvocations(topic, relevance, filter);

        topicFilter.preRemove();
        assertFalse(topicFilter.getTopic().isPresent());
        assertFalse(topicFilter.getRelevance().isPresent());
        assertFalse(topicFilter.getFilter().isPresent());

        verify(relevance).removeTopicFilter(topicFilter);
        verify(topic).removeTopicFilter(topicFilter);
        verify(filter).removeTopicFilter(topicFilter);
    }

    @Test
    public void testToString() throws URISyntaxException {
        final var filterPublicId = new URI("urn:filter-id1");
        final var topicPublicId = new URI("urn:topic-id1");

        final var relevance = mock(Relevance.class);
        final var topic = mock(Topic.class);
        final var filter = mock(Filter.class);

        when(relevance.getName()).thenReturn("relevance-name");
        when(topic.getPublicId()).thenReturn(topicPublicId);
        when(topic.getName()).thenReturn("topic-name");
        when(filter.getName()).thenReturn("filter-name");
        when(filter.getPublicId()).thenReturn(filterPublicId);

        topicFilter.setFilter(filter);
        topicFilter.setTopic(topic);
        topicFilter.setRelevance(relevance);

        assertEquals("TopicFilter: { topic-name urn:topic-id1 --relevance-name--> filter-name urn:filter-id1 }", topicFilter.toString());
    }
}