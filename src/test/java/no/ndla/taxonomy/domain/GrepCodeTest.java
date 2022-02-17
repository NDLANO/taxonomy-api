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

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class GrepCodeTest {
    private GrepCode grepCode;

    @BeforeEach
    void setUp() {
        grepCode = new GrepCode();
    }

    @Test
    void getId() {
        final var id = randomUUID();
        setField(grepCode, "id", id);

        assertEquals(id, grepCode.getId());
    }

    @Test
    void getCode() {
        setField(grepCode, "code", "KM4545");
        assertEquals("KM4545", grepCode.getCode());
    }

    @Test
    void setCode() {
        grepCode.setCode("KM4646");
        assertEquals("KM4646", getField(grepCode, "code"));
    }

    @Test
    void addGetRemoveContainsMetadata() {
        final var metadata1 = mock(Metadata.class);
        final var metadata2 = mock(Metadata.class);

        assertFalse(grepCode.containsMetadata(metadata1));
        assertFalse(grepCode.containsMetadata(metadata2));
        assertEquals(0, grepCode.getMetadata().size());

        grepCode.addMetadata(metadata1);

        assertTrue(grepCode.containsMetadata(metadata1));
        assertFalse(grepCode.containsMetadata(metadata2));
        assertEquals(1, grepCode.getMetadata().size());

        grepCode.addMetadata(metadata2);

        assertTrue(grepCode.containsMetadata(metadata1));
        assertTrue(grepCode.containsMetadata(metadata2));
        assertEquals(2, grepCode.getMetadata().size());

        assertTrue(grepCode.getMetadata().containsAll(Set.of(metadata1, metadata2)));

        grepCode.removeMetadata(metadata1);

        assertFalse(grepCode.containsMetadata(metadata1));
        assertTrue(grepCode.containsMetadata(metadata2));
        assertEquals(1, grepCode.getMetadata().size());

        grepCode.removeMetadata(metadata2);

        assertFalse(grepCode.containsMetadata(metadata1));
        assertFalse(grepCode.containsMetadata(metadata2));
        assertEquals(0, grepCode.getMetadata().size());
    }
}
