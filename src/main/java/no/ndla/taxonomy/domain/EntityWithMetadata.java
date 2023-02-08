/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EntityWithMetadata {
    Metadata getMetadata();

    default void setMetadata(Metadata metadata) {
        setCustomFields(metadata.getCustomFields());
        setGrepCodes(metadata.getGrepCodes());
        setVisible(metadata.isVisible());
        setUpdatedAt(metadata.getUpdatedAt());
        setCreatedAt(metadata.getCreatedAt());
        metadata.setParent(this);
    }

    Set<JsonGrepCode> getGrepCodes();

    boolean getVisible();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    Map<String, String> getCustomFields();

    void setCustomField(String key, String value);

    void unsetCustomField(String key);

    void setGrepCodes(Set<JsonGrepCode> codes);

    void setCustomFields(Map<String, String> customFields);

    void setVisible(boolean visible);

    void setUpdatedAt(Instant updatedAt);

    void setCreatedAt(Instant createdAt);
}
