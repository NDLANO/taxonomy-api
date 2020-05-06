package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class RelevanceTranslationTest {
    private RelevanceTranslation relevanceTranslation;

    @BeforeEach
    public void setUp() {
        relevanceTranslation = new RelevanceTranslation();
    }

    @Test
    public void testConstructor() {
        var relevance = mock(Relevance.class);
        var relevanceTranslation2 = new RelevanceTranslation(relevance, "en");
        assertEquals("en", relevanceTranslation2.getLanguageCode());
        assertEquals(relevance, relevanceTranslation2.getRelevance());
        verify(relevance).addTranslation(relevanceTranslation2);
    }

    @Test
    public void getAndSetRelevance() {
        var relevance1 = mock(Relevance.class);
        var relevance2 = mock(Relevance.class);

        when(relevance1.getTranslations()).thenReturn(Set.of());
        when(relevance2.getTranslations()).thenReturn(Set.of());

        relevanceTranslation.setRelevance(relevance1);
        verify(relevance1).addTranslation(relevanceTranslation);

        when(relevance1.getTranslations()).thenReturn(Set.of(relevanceTranslation));

        relevanceTranslation.setRelevance(relevance2);
        verify(relevance2).addTranslation(relevanceTranslation);
        verify(relevance1).removeTranslation(relevanceTranslation);
    }

    @Test
    public void setAndGetName() {
        assertNull(relevanceTranslation.getName());

        relevanceTranslation.setName("test1");
        assertEquals("test1", relevanceTranslation.getName());
    }

    @Test
    public void getLanguageCode() {
        setField(relevanceTranslation, "languageCode", "nb");
        assertEquals("nb", relevanceTranslation.getLanguageCode());
    }
}