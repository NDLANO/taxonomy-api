/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.CustomField;
import no.ndla.taxonomy.domain.CustomFieldValue;

import java.util.Optional;

public class MetadataFilters {
    private Optional<String> key;
    private Optional<String> value;
    private Optional<Boolean> visible;

    public MetadataFilters(Optional<String> key, Optional<String> value, Optional<Boolean> visible) {
        this.key = key;
        this.value = value;
        this.visible = visible;
    }

    public Optional<String> getKey() {
        return key;
    }

    public Optional<String> getValue() {
        return value;
    }

    public Optional<Boolean> getVisible() {
        return visible;
    }

}
