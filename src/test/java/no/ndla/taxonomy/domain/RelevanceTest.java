package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;

public class RelevanceTest {
    private Relevance relevance;

    @Before
    public void setUp() {
        this.relevance = new Relevance();
    }

    @Test
    public void addAndGetAndRemoveTranslations() {
        assertEquals(0, relevance.getTranslations().size());

        var returnedTranslation = relevance.addTranslation("nb");
        assertEquals(1, relevance.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(relevance.getTranslations().contains(returnedTranslation));
        assertEquals(relevance, returnedTranslation.getRelevance());

        var returnedTranslation2 = relevance.addTranslation("en");
        assertEquals(2, ((Collection) getField(relevance, "translations")).size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(relevance.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(relevance, returnedTranslation2.getRelevance());

        relevance.removeTranslation("nb");

        assertNull(returnedTranslation.getRelevance());
        assertFalse(relevance.getTranslations().contains(returnedTranslation));

        assertFalse(relevance.getTranslation("nb").isPresent());

        relevance.addTranslation(returnedTranslation);
        assertEquals(relevance, returnedTranslation.getRelevance());
        assertTrue(relevance.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, relevance.getTranslation("nb").get());
        assertEquals(returnedTranslation2, relevance.getTranslation("en").get());
    }

    @Test
    public void getAddAndRemoveResourceFilters() {
        assertEquals(0, relevance.getResourceFilters().size());

        final var resourceFilter1 = mock(ResourceFilter.class);
        final var resourceFilter2 = mock(ResourceFilter.class);

        final var relevance2 = mock(Relevance.class);

        when(resourceFilter1.getRelevance()).thenReturn(Optional.empty());
        when(resourceFilter2.getRelevance()).thenReturn(Optional.ofNullable(relevance2));

        try {
            relevance.addResourceFilter(resourceFilter1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(resourceFilter1.getRelevance()).thenReturn(Optional.ofNullable(relevance));
        relevance.addResourceFilter(resourceFilter1);

        assertEquals(1, relevance.getResourceFilters().size());
        assertTrue(relevance.getResourceFilters().contains(resourceFilter1));

        try {
            relevance.addResourceFilter(resourceFilter2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(resourceFilter2.getRelevance()).thenReturn(Optional.ofNullable(relevance));
        relevance.addResourceFilter(resourceFilter2);

        assertEquals(2, relevance.getResourceFilters().size());
        assertTrue(relevance.getResourceFilters().containsAll(Set.of(resourceFilter1, resourceFilter2)));

        relevance.removeResourceFilter(resourceFilter1);

        verify(resourceFilter1).disassociate();

        assertEquals(1, relevance.getResourceFilters().size());
        assertTrue(relevance.getResourceFilters().contains(resourceFilter2));
    }

    @Test
    public void getAddAndRemoveTopicFilters() {
        assertEquals(0, relevance.getTopicFilters().size());

        final var topicFilter1 = mock(TopicFilter.class);
        final var topicFilter2 = mock(TopicFilter.class);

        final var relevance2 = mock(Relevance.class);

        when(topicFilter1.getRelevance()).thenReturn(Optional.empty());
        when(topicFilter2.getRelevance()).thenReturn(Optional.ofNullable(relevance2));

        try {
            relevance.addTopicFilter(topicFilter1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicFilter1.getRelevance()).thenReturn(Optional.ofNullable(relevance));
        relevance.addTopicFilter(topicFilter1);

        assertEquals(1, relevance.getTopicFilters().size());
        assertTrue(relevance.getTopicFilters().contains(topicFilter1));

        try {
            relevance.addTopicFilter(topicFilter2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        when(topicFilter2.getRelevance()).thenReturn(Optional.ofNullable(relevance));
        relevance.addTopicFilter(topicFilter2);

        assertEquals(2, relevance.getTopicFilters().size());
        assertTrue(relevance.getTopicFilters().containsAll(Set.of(topicFilter1, topicFilter2)));

        relevance.removeTopicFilter(topicFilter1);

        verify(topicFilter1).disassociate();

        assertEquals(1, relevance.getTopicFilters().size());
        assertTrue(relevance.getTopicFilters().contains(topicFilter2));
    }


}