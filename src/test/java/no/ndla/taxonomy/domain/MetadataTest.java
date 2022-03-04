/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class MetadataTest {
    private Metadata metadata;

    @BeforeEach
    void setUp() {
        metadata = new Metadata();
    }

    @Test
    void getId() {
        final var id = 1;
        setField(metadata, "id", id);
        assertEquals(id, metadata.getId());
    }

    @Test
    void addGetAndRemoveGrepCode() {
        final var grepCode1 = mock(GrepCode.class);
        final var grepCode2 = mock(GrepCode.class);

        when(grepCode1.containsMetadata(metadata)).thenReturn(false);
        when(grepCode2.containsMetadata(metadata)).thenReturn(false);

        assertEquals(0, metadata.getGrepCodes().size());

        metadata.addGrepCode(grepCode1);

        when(grepCode1.containsMetadata(metadata)).thenReturn(true);

        verify(grepCode1).addMetadata(metadata);

        assertEquals(1, metadata.getGrepCodes().size());
        assertTrue(metadata.getGrepCodes().contains(grepCode1));

        metadata.addGrepCode(grepCode2);

        when(grepCode2.containsMetadata(metadata)).thenReturn(true);

        verify(grepCode2).addMetadata(metadata);

        assertEquals(2, metadata.getGrepCodes().size());
        assertTrue(metadata.getGrepCodes().contains(grepCode1));
        assertTrue(metadata.getGrepCodes().contains(grepCode2));

        metadata.removeGrepCode(grepCode1);

        verify(grepCode1).removeMetadata(metadata);

        assertEquals(1, metadata.getGrepCodes().size());
        assertTrue(metadata.getGrepCodes().contains(grepCode2));

        metadata.removeGrepCode(grepCode2);

        verify(grepCode2).removeMetadata(metadata);

        assertEquals(0, metadata.getGrepCodes().size());
    }

    @Test
    void preRemove() {
        final var aim1 = mock(GrepCode.class);
        final var aim2 = mock(GrepCode.class);

        metadata.addGrepCode(aim1);
        metadata.addGrepCode(aim2);

        reset(aim1, aim2);

        when(aim1.containsMetadata(metadata)).thenReturn(true);
        when(aim2.containsMetadata(metadata)).thenReturn(true);

        assertTrue(metadata.getGrepCodes().containsAll(Set.of(aim1, aim2)));

        metadata.preRemove();

        verify(aim1).removeMetadata(metadata);
        verify(aim2).removeMetadata(metadata);

        assertEquals(0, metadata.getGrepCodes().size());
    }

    @Test
    void isVisible() {
        assertTrue(metadata.isVisible());

        setField(metadata, "visible", false);
        assertFalse(metadata.isVisible());

        setField(metadata, "visible", true);
        assertTrue(metadata.isVisible());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void setVisible() {
        assertTrue((boolean) getField(metadata, "visible"));

        metadata.setVisible(false);
        assertFalse((boolean) getField(metadata, "visible"));

        metadata.setVisible(true);
        assertTrue((boolean) getField(metadata, "visible"));
    }
}
