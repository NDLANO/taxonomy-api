package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class FilterTest {
    private Filter filter;

    @Before
    public void setUp() {
        this.filter = new Filter();
    }

    @Test
    public void addAndGetAndRemoveTranslations() {
        assertEquals(0, filter.getTranslations().size());

        var returnedTranslation = filter.addTranslation("nb");
        assertEquals(1, filter.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(filter.getTranslations().contains(returnedTranslation));
        assertEquals(filter, returnedTranslation.getFilter());

        var returnedTranslation2 = filter.addTranslation("en");
        assertEquals(2, ((Collection) getField(filter, "translations")).size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(filter.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(filter, returnedTranslation2.getFilter());

        filter.removeTranslation("nb");

        assertNull(returnedTranslation.getFilter());
        assertFalse(filter.getTranslations().contains(returnedTranslation));

        assertFalse(filter.getTranslation("nb").isPresent());

        filter.addTranslation(returnedTranslation);
        assertEquals(filter, returnedTranslation.getFilter());
        assertTrue(filter.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, filter.getTranslation("nb").get());
        assertEquals(returnedTranslation2, filter.getTranslation("en").get());
    }

    @Test
    public void setAndGetSubject() {
        assertFalse(filter.getSubject().isPresent());

        final var subject = mock(Subject.class);
        when(subject.getFilters()).thenReturn(new HashSet<>());

        filter.setSubject(subject);

        verify(subject).addFilter(filter);

        assertTrue(filter.getSubject().isPresent());
        assertEquals(subject, filter.getSubject().get());

        verify(subject, times(0)).removeFilter(filter);

        final var subject2 = mock(Subject.class);
        when(subject2.getFilters()).thenReturn(new HashSet<>());

        when(subject.getFilters()).thenReturn(Set.of(filter));

        filter.setSubject(subject2);

        verify(subject).removeFilter(filter);

        verify(subject2).addFilter(filter);

        verify(subject2, times(0)).removeFilter(filter);

        when(subject2.getFilters()).thenReturn(Set.of(filter));

        filter.setSubject(null);

        verify(subject2).removeFilter(filter);
    }

    @Test
    public void addGetAndRemoveResourceFilter() {
        assertEquals(0, filter.getResourceFilters().size());

        final var resourceFilter1 = mock(ResourceFilter.class);
        final var resourceFilter2 = mock(ResourceFilter.class);

        final var filter2 = mock(Filter.class);

        when(resourceFilter1.getFilter()).thenReturn(null);
        when(resourceFilter2.getFilter()).thenReturn(filter2);

        try {
            filter.addResourceFilter(resourceFilter1);
        } catch (IllegalArgumentException ignored) {
        }
        when(resourceFilter1.getFilter()).thenReturn(filter);
        filter.addResourceFilter(resourceFilter1);

        assertEquals(1, filter.getResourceFilters().size());
        assertTrue(filter.getResourceFilters().contains(resourceFilter1));

        try {
            filter.addResourceFilter(resourceFilter2);
        } catch (IllegalArgumentException ignored) {
        }
        when(resourceFilter2.getFilter()).thenReturn(filter);
        filter.addResourceFilter(resourceFilter2);

        assertEquals(2, filter.getResourceFilters().size());
        assertTrue(filter.getResourceFilters().containsAll(Set.of(resourceFilter1, resourceFilter2)));

        filter.removeResourceFilter(resourceFilter1);

        verify(resourceFilter1).disassociate();

        assertEquals(1, filter.getResourceFilters().size());
        assertTrue(filter.getResourceFilters().contains(resourceFilter2));
    }

    @Test
    public void addGetAndRemoveTopicFilter() {
        assertEquals(0, filter.getTopicFilters().size());

        final var topicFilter1 = mock(TopicFilter.class);
        final var topicFilter2 = mock(TopicFilter.class);

        final var filter2 = mock(Filter.class);

        when(topicFilter1.getFilter()).thenReturn(Optional.empty());
        when(topicFilter2.getFilter()).thenReturn(Optional.of(filter2));

        try {
            filter.addTopicFilter(topicFilter1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicFilter1.getFilter()).thenReturn(Optional.of(filter));
        filter.addTopicFilter(topicFilter1);

        assertEquals(1, filter.getTopicFilters().size());
        assertTrue(filter.getTopicFilters().contains(topicFilter1));

        try {
            filter.addTopicFilter(topicFilter2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicFilter2.getFilter()).thenReturn(Optional.of(filter));
        filter.addTopicFilter(topicFilter2);

        assertEquals(2, filter.getTopicFilters().size());
        assertTrue(filter.getTopicFilters().containsAll(Set.of(topicFilter1, topicFilter2)));

        filter.removeTopicFilter(topicFilter1);

        verify(topicFilter1).disassociate();

        assertEquals(1, filter.getTopicFilters().size());
        assertTrue(filter.getTopicFilters().contains(topicFilter2));
    }

    @Test
    public void preRemove() {
        var filterSpy = spy(filter);

        final var resourceFilter1 = mock(ResourceFilter.class);
        final var resourceFilter2 = mock(ResourceFilter.class);
        final var resourceFilter3 = mock(ResourceFilter.class);

        final var topicFilter1 = mock(TopicFilter.class);
        final var topicFilter2 = mock(TopicFilter.class);
        final var topicFilter3 = mock(TopicFilter.class);

        setField(filterSpy, "resources", new HashSet<>(Set.of(resourceFilter1, resourceFilter2, resourceFilter3)));
        setField(filterSpy, "topics", new HashSet<>(Set.of(topicFilter1, topicFilter2, topicFilter3)));

        filterSpy.preRemove();

        verify(filterSpy).removeTopicFilter(topicFilter1);
        verify(filterSpy).removeTopicFilter(topicFilter2);
        verify(filterSpy).removeTopicFilter(topicFilter3);

        verify(filterSpy).removeResourceFilter(resourceFilter1);
        verify(filterSpy).removeResourceFilter(resourceFilter2);
        verify(filterSpy).removeResourceFilter(resourceFilter3);
    }
}