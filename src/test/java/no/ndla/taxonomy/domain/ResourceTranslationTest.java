/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ResourceTranslationTest {
    private ResourceTranslation resourceTranslation;

    @BeforeEach
    public void setUp() {
        resourceTranslation = new ResourceTranslation();
    }

    @Test
    public void testConstructor() {
        var resource = mock(Resource.class);
        var resourceTranslation2 = new ResourceTranslation(resource, "en");
        assertEquals("en", resourceTranslation2.getLanguageCode());
        assertEquals(resource, resourceTranslation2.getResource());
        verify(resource).addTranslation(resourceTranslation2);
    }

    @Test
    public void getAndSetResource() {
        var resource1 = mock(Resource.class);
        var resource2 = mock(Resource.class);

        when(resource1.getTranslations()).thenReturn(Set.of());
        when(resource2.getTranslations()).thenReturn(Set.of());

        resourceTranslation.setResource(resource1);
        verify(resource1).addTranslation(resourceTranslation);

        when(resource1.getTranslations()).thenReturn(Set.of(resourceTranslation));

        resourceTranslation.setResource(resource2);
        verify(resource2).addTranslation(resourceTranslation);
        verify(resource1).removeTranslation(resourceTranslation);
    }

    @Test
    public void setAndGetName() {
        assertNull(resourceTranslation.getName());

        resourceTranslation.setName("test1");
        assertEquals("test1", resourceTranslation.getName());
    }

    @Test
    public void getLanguageCode() {
        setField(resourceTranslation, "languageCode", "nb");
        assertEquals("nb", resourceTranslation.getLanguageCode());
    }
}