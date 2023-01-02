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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ResourceTypeTest {
    private ResourceType resourceType;

    @BeforeEach
    public void setUp() {
        resourceType = new ResourceType();
    }

    @Test
    public void testConstructor() {
        final var createdResourceType = new ResourceType();
        assertNotNull(createdResourceType.getPublicId());
        assertTrue(createdResourceType.getPublicId().toString().length() > 4);
    }

    @Test
    public void name() {
        assertEquals(resourceType, resourceType.name("testname"));
        assertEquals("testname", resourceType.getName());
    }

    @Test
    public void setAndGetParent() {
        final var parent1 = new ResourceType();
        final var parent2 = new ResourceType();

        assertFalse(resourceType.getParent().isPresent());

        resourceType.setParent(parent1);
        assertEquals(parent1, resourceType.getParent().orElse(null));
        assertTrue(parent1.getSubtypes().contains(resourceType));
        assertFalse(parent2.getSubtypes().contains(resourceType));

        resourceType.setParent(parent2);
        assertEquals(parent2, resourceType.getParent().orElse(null));
        assertFalse(parent1.getSubtypes().contains(resourceType));
        assertTrue(parent2.getSubtypes().contains(resourceType));

        resourceType.setParent(null);
        assertFalse(resourceType.getParent().isPresent());
        assertFalse(parent1.getSubtypes().contains(resourceType));
        assertFalse(parent2.getSubtypes().contains(resourceType));
    }

    @Test
    public void addGetAndRemoveSubtype() {
        final var subType1 = new ResourceType();
        final var subType2 = new ResourceType();

        assertEquals(0, resourceType.getSubtypes().size());

        resourceType.addSubtype(subType1);
        assertEquals(1, resourceType.getSubtypes().size());
        assertTrue(resourceType.getSubtypes().contains(subType1));
        assertFalse(resourceType.getSubtypes().contains(subType2));
        assertEquals(resourceType, subType1.getParent().orElse(null));
        assertFalse(subType2.getParent().isPresent());

        resourceType.addSubtype(subType2);
        assertEquals(2, resourceType.getSubtypes().size());
        assertTrue(resourceType.getSubtypes().contains(subType1));
        assertTrue(resourceType.getSubtypes().contains(subType2));
        assertEquals(resourceType, subType1.getParent().orElse(null));
        assertEquals(resourceType, subType2.getParent().orElse(null));

        resourceType.removeSubType(subType1);
        assertEquals(1, resourceType.getSubtypes().size());
        assertFalse(resourceType.getSubtypes().contains(subType1));
        assertTrue(resourceType.getSubtypes().contains(subType2));
        assertFalse(subType1.getParent().isPresent());
        assertEquals(resourceType, subType2.getParent().orElse(null));

        resourceType.removeSubType(subType2);
        assertEquals(0, resourceType.getSubtypes().size());
        assertFalse(subType1.getParent().isPresent());
        assertFalse(subType2.getParent().isPresent());
    }

    @Test
    public void getAddAndRemoveTranslation() {
        assertEquals(0, resourceType.getTranslations().size());

        var returnedTranslation = resourceType.addTranslation("nb");
        assertEquals(1, resourceType.getTranslations().size());
        assertEquals("nb", returnedTranslation.getLanguageCode());
        assertTrue(resourceType.getTranslations().contains(returnedTranslation));
        assertEquals(resourceType, returnedTranslation.getResourceType());

        var returnedTranslation2 = resourceType.addTranslation("en");
        assertEquals(2, resourceType.getTranslations().size());
        assertEquals("en", returnedTranslation2.getLanguageCode());
        assertTrue(resourceType.getTranslations().containsAll(Set.of(returnedTranslation, returnedTranslation2)));
        assertEquals(resourceType, returnedTranslation2.getResourceType());

        resourceType.removeTranslation("nb");

        assertNull(returnedTranslation.getResourceType());
        assertFalse(resourceType.getTranslations().contains(returnedTranslation));

        assertFalse(resourceType.getTranslation("nb").isPresent());

        resourceType.addTranslation(returnedTranslation);
        assertEquals(resourceType, returnedTranslation.getResourceType());
        assertTrue(resourceType.getTranslations().contains(returnedTranslation));

        assertEquals(returnedTranslation, resourceType.getTranslation("nb").get());
        assertEquals(returnedTranslation2, resourceType.getTranslation("en").orElse(null));
    }
}
