/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.JsonGrepCode;
import no.ndla.taxonomy.domain.Metadata;

@Schema(name = "Metadata")
public class MetadataDTO {
    @JsonIgnore
    public String publicId;

    @Schema
    public Set<String> grepCodes;

    @Schema
    public Boolean visible;

    @Schema
    public Map<String, String> customFields;

    public MetadataDTO() {}

    public MetadataDTO(Metadata metadata) {
        this.visible = metadata.isVisible();
        this.grepCodes =
                metadata.getGrepCodes().stream().map(JsonGrepCode::code).collect(Collectors.toSet());
        this.customFields = metadata.getCustomFields();
    }

    public MetadataDTO(MetadataApiEntity metadataApiEntity) {
        this.publicId = metadataApiEntity.getPublicId();
        this.visible = metadataApiEntity.isVisible().orElse(null);

        metadataApiEntity.getCompetenceAims().ifPresent(competenceAims -> {
            grepCodes = new HashSet<>();
            competenceAims.stream()
                    .map(MetadataApiEntity.CompetenceAim::getCode)
                    .forEach(this::addGrepCode);
        });
        customFields = metadataApiEntity.getCustomFields().map(HashMap::new).orElse(null);
    }

    public static MetadataDTO of(MetadataDTO metadataDto) {
        final var newMetadataDto = new MetadataDTO();
        newMetadataDto.setPublicId(metadataDto.getPublicId());
        newMetadataDto.setGrepCodes(metadataDto.getGrepCodes());
        newMetadataDto.setVisible(metadataDto.isVisible());
        {
            final var customFields = metadataDto.getCustomFields();
            newMetadataDto.setCustomFields(customFields != null ? new HashMap<>(customFields) : null);
        }

        return newMetadataDto;
    }

    private void addGrepCode(String grepCode) {
        if (this.grepCodes == null) {
            grepCodes = new HashSet<>();
        }

        grepCodes.add(grepCode);
    }

    public Set<String> getGrepCodes() {
        return grepCodes;
    }

    public void setGrepCodes(Set<String> grepCodes) {
        this.grepCodes = grepCodes;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Boolean isVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields;
    }
}
