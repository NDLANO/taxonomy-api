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
import java.util.UUID;

public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, UUID> {
    @Query("SELECT obj FROM CustomFieldValue obj WHERE obj.metadata.id = :metadata AND obj.customField.id = :customField")
    Optional<CustomFieldValue> findByMetadataAndCustomField(UUID metadata, UUID customField);

    @Query("SELECT obj FROM CustomFieldValue obj WHERE obj.metadata.id = :metadata")
    Iterable<CustomFieldValue> findAllByMetadata(UUID metadata);

    @Query("SELECT obj FROM CustomFieldValue obj LEFT JOIN FETCH obj.metadata WHERE obj.customField.id = :customField AND obj.value = :value")
    Iterable<CustomFieldValue> findAllByCustomFieldAndValue(UUID customField, String value);

    @Query("SELECT obj FROM CustomFieldValue obj LEFT JOIN FETCH obj.metadata WHERE obj.customField.id = :customField")
    Iterable<CustomFieldValue> findAllByCustomField(UUID customField);
}
