/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

public class MetadataApiEntity {
    private String publicId;

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<CompetenceAim> competenceAims = new HashSet<>();

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean visible;

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> customFields;

    public MetadataApiEntity() {}

    public MetadataApiEntity(MetadataDto entityMetadataObject) {
        if (entityMetadataObject.getGrepCodes() != null) {
            this.competenceAims = new HashSet<>();
            entityMetadataObject
                    .getGrepCodes()
                    .forEach(aim -> addCompetenceAim(new CompetenceAim(aim)));
        } else {
            competenceAims = null;
        }

        {
            final var customFields = entityMetadataObject.getCustomFields();
            if (customFields != null) {
                this.customFields = new HashMap<>(customFields);
            } else {
                this.customFields = null;
            }
        }

        this.visible = entityMetadataObject.isVisible();
        this.publicId = entityMetadataObject.getPublicId();
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    @JsonIgnore
    public Optional<Set<CompetenceAim>> getCompetenceAims() {
        if (competenceAims == null) {
            return Optional.empty();
        }

        return Optional.of(competenceAims.stream().collect(Collectors.toUnmodifiableSet()));
    }

    public Optional<Boolean> isVisible() {
        return Optional.ofNullable(visible);
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    void setCompetenceAims(Set<CompetenceAim> competenceAims) {
        this.competenceAims = competenceAims;
    }

    public void addCompetenceAim(CompetenceAim competenceAim) {
        if (competenceAims == null) {
            competenceAims = new HashSet<>();
        }

        this.competenceAims.add(competenceAim);
    }

    public void removeCompetenceAim(CompetenceAim competenceAim) {
        if (competenceAims == null) {
            return;
        }

        this.competenceAims.remove(competenceAim);
    }

    @JsonIgnore
    public Optional<Map<String, String>> getCustomFields() {
        return Optional.ofNullable(
                customFields != null ? Collections.unmodifiableMap(customFields) : null);
    }

    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields;
    }

    public static class CompetenceAim {
        private String code;

        CompetenceAim() {}

        CompetenceAim(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
