/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        var returnedTranslation2 = relevance.addTranslation("en");
        assertEquals(2, ((Collection) getField(relevance, "translations")).size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(relevance.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));

        relevance.removeTranslation("nb");

        assertFalse(relevance.getTranslations().contains(returnedTranslation));

        assertFalse(relevance.getTranslation("nb").isPresent());

        relevance.addTranslation(returnedTranslation);
        assertTrue(relevance.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, relevance.getTranslation("nb").get());
        assertEquals(returnedTranslation2, relevance.getTranslation("en").get());
    }
}
