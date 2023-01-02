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
import no.ndla.taxonomy.service.exceptions.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CustomFieldServiceImpl implements CustomFieldService {
    private final CustomFieldRepository customFieldRepository;
    private final CustomFieldValueRepository customFieldValueRepository;

    public CustomFieldServiceImpl(CustomFieldRepository customFieldRepository,
            CustomFieldValueRepository customFieldValueRepository) {
        this.customFieldRepository = customFieldRepository;
        this.customFieldValueRepository = customFieldValueRepository;
    }

    @Override
    @Transactional
    public void setCustomField(final Metadata metadata, final String customField, final String value) {
        final CustomField customFieldObject = customFieldRepository.findByKey(customField).orElseGet(() -> {
            CustomField customFieldObj = new CustomField();
            customFieldObj.setKey(customField);
            return customFieldRepository.saveAndFlush(customFieldObj);
        });
        if (value != null) {
            final CustomFieldValue valueObject = customFieldValueRepository
                    .findByMetadataAndCustomField(metadata.getId(), customFieldObject.getId()).orElseGet(() -> {
                        CustomFieldValue newObject = new CustomFieldValue();
                        newObject.setCustomField(customFieldObject);
                        newObject.setMetadata(metadata);
                        return newObject;
                    });
            valueObject.setValue(value);
            customFieldValueRepository.save(valueObject);
            metadata.addCustomFieldValue(valueObject);
        }
    }

    @Override
    @Transactional
    public Map<String, FieldValue> getCustomFields(Metadata metadata) {
        return StreamSupport.stream(customFieldValueRepository.findAllByMetadata(metadata.getId()).spliterator(), false)
                .collect(Collectors.toMap(value -> value.getCustomField().getKey(),
                        value -> new FieldValueImpl(value.getId(), value.getValue())));
    }

    @Override
    public void unsetCustomField(Integer id) throws EntityNotFoundException {
        customFieldValueRepository
                .delete(customFieldValueRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)));
    }

    @Override
    public List<Metadata> getMetadataListByCustomFieldKeyValue(String key, String value) {
        final Integer customFieldId;
        {
            final CustomField customField;
            {
                final var opt = customFieldRepository.findByKey(key);
                if (opt.isEmpty()) {
                    return List.of();
                }
                customField = opt.get();
            }
            customFieldId = customField.getId();
        }

        Iterable<CustomFieldValue> customFields;
        if (value != null)
            customFields = customFieldValueRepository.findAllByCustomFieldAndValue(customFieldId, value);
        else
            customFields = customFieldValueRepository.findAllByCustomField(customFieldId);

        return StreamSupport.stream(customFields.spliterator(), false).map(CustomFieldValue::getMetadata)
                .collect(Collectors.toList());
    }

    static class FieldValueImpl implements FieldValue {
        private final Integer id;
        private final String value;

        FieldValueImpl(Integer id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public String getValue() {
            return value;
        }
    }
}
