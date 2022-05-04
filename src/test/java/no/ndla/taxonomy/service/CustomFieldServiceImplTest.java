/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.CustomField;
import no.ndla.taxonomy.domain.CustomFieldValue;
import no.ndla.taxonomy.domain.Metadata;
import no.ndla.taxonomy.repositories.CustomFieldRepository;
import no.ndla.taxonomy.repositories.CustomFieldValueRepository;
import no.ndla.taxonomy.repositories.MetadataRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class CustomFieldServiceImplTest {
    private MetadataRepository metadataRepository;
    private CustomFieldRepository customFieldRepository;
    private CustomFieldValueRepository customFieldValueRepository;
    private CustomFieldServiceImpl customFieldService;

    @BeforeEach
    public void setUp(@Autowired MetadataRepository metadataRepository,
            @Autowired CustomFieldRepository customFieldRepository,
            @Autowired CustomFieldValueRepository customFieldValueRepository) {
        this.metadataRepository = metadataRepository;
        this.customFieldRepository = customFieldRepository;
        this.customFieldValueRepository = customFieldValueRepository;
        this.customFieldService = new CustomFieldServiceImpl(customFieldRepository, customFieldValueRepository);
        metadataRepository.deleteAll();
        customFieldRepository.deleteAll();
        customFieldValueRepository.deleteAll();
    }

    @AfterAll
    public static void cleanUp(@Autowired NodeRepository nodeRepository,
            @Autowired MetadataRepository metadataRepository, @Autowired CustomFieldRepository customFieldRepository) {
        nodeRepository.deleteAll();
        metadataRepository.deleteAll();
        customFieldRepository.deleteAll();
    }

    @Test
    public void testSetCustomFieldForTheFirstTime() {
        assertFalse(customFieldRepository.findByKey("new-field").isPresent());
        Metadata metadata = new Metadata();
        metadata = metadataRepository.save(metadata);
        assertNotNull(metadata.getId());
        assertTrue(customFieldService.getCustomFields(metadata).isEmpty());
        customFieldService.setCustomField(metadata, "new-field", "A Value");
        final var customField = customFieldRepository.findByKey("new-field").orElse(null);
        assertNotNull(customField);
        assertNotNull(customField.getId());
        final var customFieldValue = customFieldValueRepository
                .findByMetadataAndCustomField(metadata.getId(), customField.getId()).orElse(null);
        assertNotNull(customFieldValue);
        assertNotNull(customFieldValue.getId());
        assertNotNull(customFieldValue.getMetadata());
        assertNotNull(customFieldValue.getCustomField());
        assertEquals(metadata.getId(), customFieldValue.getMetadata().getId());
        assertEquals(customField.getId(), customFieldValue.getCustomField().getId());
        assertEquals("new-field", customFieldValue.getCustomField().getKey());
        assertEquals("A Value", customFieldValue.getValue());
    }

    @Test
    public void testSetCustomField() {
        CustomField customField = new CustomField();
        customField.setKey("new-field");
        customField = customFieldRepository.save(customField);
        assertNotNull(customField.getId());
        assertNotNull(customField.getCreatedAt());
        Metadata metadata = new Metadata();
        metadata = metadataRepository.save(metadata);
        assertNotNull(metadata.getId());
        assertTrue(customFieldService.getCustomFields(metadata).isEmpty());
    }

    @Test
    public void testGetCustomFieldValuesAndDelete() throws EntityNotFoundException {
        CustomField customField = new CustomField();
        customField.setKey("new-field");
        customField = customFieldRepository.save(customField);
        assertNotNull(customField.getId());
        assertNotNull(customField.getCreatedAt());
        Metadata metadata = new Metadata();
        metadata = metadataRepository.save(metadata);
        assertNotNull(metadata.getId());
        CustomFieldValue customFieldValue = new CustomFieldValue();
        customFieldValue.setCustomField(customField);
        customFieldValue.setMetadata(metadata);
        customFieldValue.setValue("A value");
        customFieldValue = customFieldValueRepository.save(customFieldValue);
        assertNotNull(customFieldValue.getId());
        Map<String, CustomFieldService.FieldValue> values = customFieldService.getCustomFields(metadata);
        assertFalse(values.isEmpty());
        final var value = values.get("new-field");
        assertEquals(customFieldValue.getId(), value.getId());
        assertEquals("A value", value.getValue());
        customFieldService.unsetCustomField(value.getId());
        // assertTrue(customFieldService.getCustomFields(metadata).isEmpty());
    }

    @Test
    public void testDeleteUnknownValue() {
        final var id = 1;
        assertThrows(EntityNotFoundException.class, () -> {
            customFieldService.unsetCustomField(id);
        });
    }

    @Test
    public void testGetByKeyValueKeyNotFound() {
        assertTrue(customFieldService.getMetadataListByCustomFieldKeyValue("testkey", "testvalue").isEmpty());
    }

    @Test
    public void testGetByKeyValueNoValues() {
        CustomField customField = new CustomField();
        customField.setKey("testkey");
        customField = customFieldRepository.save(customField);
        assertTrue(customFieldService.getMetadataListByCustomFieldKeyValue("testkey", "testvalue").isEmpty());
    }

    @Test
    public void testGetByKeyValue() {
        {
            CustomField customField = new CustomField();
            customField.setKey("testkey");
            customField = customFieldRepository.save(customField);
            Metadata metadata = new Metadata();
            metadata = metadataRepository.save(metadata);
            {
                CustomFieldValue customFieldValue = new CustomFieldValue();
                customFieldValue.setCustomField(customField);
                customFieldValue.setMetadata(metadata);
                customFieldValue.setValue("testvalue");
                customFieldValueRepository.save(customFieldValue);
            }
        }
        final var entities = customFieldService.getMetadataListByCustomFieldKeyValue("testkey", "testvalue");
        assertFalse(entities.isEmpty());
        final Metadata metadata;
        {
            final var iterator = entities.iterator();
            assertTrue(iterator.hasNext());
            metadata = iterator.next();
            assertFalse(iterator.hasNext());
        }
        assertEquals(1, metadata.getCustomFieldValues().size());
    }

    @Test
    public void testGetByKeyNullValue() {
        {
            CustomField customField = new CustomField();
            customField.setKey("testkey");
            customField = customFieldRepository.save(customField);
            Metadata metadata = new Metadata();
            metadata = metadataRepository.save(metadata);
            {
                CustomFieldValue customFieldValue = new CustomFieldValue();
                customFieldValue.setCustomField(customField);
                customFieldValue.setMetadata(metadata);
                customFieldValue.setValue("testvalue");
                customFieldValueRepository.save(customFieldValue);
            }
        }
        final var entities = customFieldService.getMetadataListByCustomFieldKeyValue("testkey", null);
        assertEquals(1, entities.size());
    }
}
