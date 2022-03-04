/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.CustomFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, Integer> {
    @Query("SELECT obj FROM CustomFieldValue obj WHERE obj.metadata.id = :metadataId AND obj.customField.id = :customFieldId")
    Optional<CustomFieldValue> findByMetadataAndCustomField(Integer metadataId, Integer customFieldId);

    @Query("SELECT obj FROM CustomFieldValue obj WHERE obj.metadata.id = :metadataId")
    Iterable<CustomFieldValue> findAllByMetadata(Integer metadataId);

    @Query("SELECT obj FROM CustomFieldValue obj LEFT JOIN FETCH obj.metadata WHERE obj.customField.id = :customFieldId AND obj.value = :value")
    Iterable<CustomFieldValue> findAllByCustomFieldAndValue(Integer customFieldId, String value);

    @Query("SELECT obj FROM CustomFieldValue obj LEFT JOIN FETCH obj.metadata WHERE obj.customField.id = :customFieldId")
    Iterable<CustomFieldValue> findAllByCustomField(Integer customFieldId);
}
