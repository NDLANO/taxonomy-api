/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.service.dtos.MetadataDto;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Metadata implements Serializable {
    private Instant updatedAt;

    private Instant createdAt;

    private Set<JsonGrepCode> grepCodes = new HashSet<>();

    private Map<String, String> customFields = new HashMap<>();

    private boolean visible = true;

    private EntityWithMetadata parent;

    public Metadata() {

    }

    public Metadata(EntityWithMetadata parent) {
        this.parent = parent;
        this.grepCodes = parent.getGrepCodes();
        this.visible = parent.getVisible();
        this.createdAt = parent.getCreatedAt();
        this.updatedAt = parent.getUpdatedAt();
        this.customFields = parent.getCustomFields();
    }

    public Metadata(Metadata metadata) {
        this.parent = metadata.parent;
        this.createdAt = metadata.createdAt;
        this.customFields = metadata.getCustomFields();
        this.grepCodes = new HashSet<>(metadata.getGrepCodes());
        this.updatedAt = metadata.updatedAt;
        this.visible = metadata.isVisible();
    }

    public Metadata(MetadataDto metadata) {
        this.customFields = metadata.getCustomFields();
        this.grepCodes = metadata.grepCodes.stream().map(JsonGrepCode::new).collect(Collectors.toSet());
        this.visible = metadata.isVisible();
    }

    public void setParent(EntityWithMetadata parent) {
        this.parent = parent;
    }

    public void addGrepCode(JsonGrepCode grepCode) {
        this.parent.getGrepCodes().add(grepCode);
    }

    public void removeGrepCode(JsonGrepCode grepCode) {
        this.grepCodes.remove(grepCode);
    }

    public Set<JsonGrepCode> getGrepCodes() {
        return new HashSet<>(grepCodes);
    }

    public Map<String, String> getCustomFields() {
        return this.customFields;
    }

    public boolean isVisible() {
        return visible;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        this.parent.setVisible(visible);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Metadata that = (Metadata) o;
        return visible == that.visible && Objects.equals(updatedAt, that.updatedAt)
                && Objects.equals(createdAt, that.createdAt) && Objects.equals(grepCodes, that.grepCodes)
                && Objects.equals(customFields, that.customFields);
    }
}
