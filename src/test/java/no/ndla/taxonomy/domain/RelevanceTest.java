/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;

public class RelevanceTest {
    private Relevance relevance;

    @BeforeEach
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
}
