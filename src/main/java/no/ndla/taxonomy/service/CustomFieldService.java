/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Metadata;
import no.ndla.taxonomy.service.exceptions.EntityNotFoundException;

import java.util.List;
import java.util.Map;

public interface CustomFieldService {
    void setCustomField(Metadata metadata, String customField, String value);

    Map<String, FieldValue> getCustomFields(Metadata metadata);

    void unsetCustomField(Integer id) throws EntityNotFoundException;

    List<Metadata> getMetadataListByCustomFieldKeyValue(String key, String value);

    interface FieldValue {
        Integer getId();

        String getValue();
    }
}
