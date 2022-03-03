/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class CustomFieldValueTest {
    private CustomFieldValue customFieldValue;

    @BeforeEach
    public void setUp() {
        customFieldValue = new CustomFieldValue();
    }

    @Test
    public void getId() {
        final var id = 1;
        setField(customFieldValue, "id", id);
        assertEquals(id, customFieldValue.getId());
    }

    @Test
    public void setId() {
        final var id = 1;
        customFieldValue.setId(id);
        assertEquals(id, getField(customFieldValue, "id"));
    }

    @Test
    public void getMetadata() {
        final var metadata = new Metadata();
        setField(customFieldValue, "metadata", metadata);
        assertSame(metadata, customFieldValue.getMetadata());
    }

    @Test
    public void setMetadata() {
        final var metadata = new Metadata();
        customFieldValue.setMetadata(metadata);
        assertSame(metadata, getField(customFieldValue, "metadata"));
    }

    @Test
    public void getCustomField() {
        final var customField = new CustomField();
        setField(customFieldValue, "customField", customField);
        assertSame(customField, customFieldValue.getCustomField());
    }

    @Test
    public void setCustomField() {
        final var customField = new CustomField();
        customFieldValue.setCustomField(customField);
        assertSame(customField, getField(customFieldValue, "customField"));
    }

    @Test
    public void getValue() {
        setField(customFieldValue, "value", "value");
        assertEquals("value", customFieldValue.getValue());
    }

    @Test
    public void setValue() {
        customFieldValue.setValue("value");
        assertEquals("value", customFieldValue.getValue());
    }
}
