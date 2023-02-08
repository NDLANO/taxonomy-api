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
    private Node parent;

    @BeforeEach
    void setUp() {
        parent = new Node(NodeType.RESOURCE);
        metadata = new Metadata(parent);
    }

    @Test
    void addGetAndRemoveGrepCode() {
        final var grepCode1 = mock(JsonGrepCode.class);
        final var grepCode2 = mock(JsonGrepCode.class);

        assertEquals(0, metadata.getGrepCodes().size());

        metadata.addGrepCode(grepCode1);

        assertEquals(1, metadata.getGrepCodes().size());
        assertTrue(metadata.getGrepCodes().contains(grepCode1));

        metadata.addGrepCode(grepCode2);

        assertEquals(2, metadata.getGrepCodes().size());
        assertTrue(metadata.getGrepCodes().contains(grepCode1));
        assertTrue(metadata.getGrepCodes().contains(grepCode2));

        metadata.removeGrepCode(grepCode1);

        assertEquals(1, metadata.getGrepCodes().size());
        assertTrue(metadata.getGrepCodes().contains(grepCode2));

        metadata.removeGrepCode(grepCode2);

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
