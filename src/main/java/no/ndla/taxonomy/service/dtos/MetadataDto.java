/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ApiModel("Metadata")
public class MetadataDto {
    @JsonIgnore
    private String publicId;

    @ApiModelProperty
    private Set<String> grepCodes;

    @ApiModelProperty
    private Boolean visible;

    @ApiModelProperty
    private Map<String, String> customFields;

    public MetadataDto() {
    }

    public MetadataDto(MetadataApiEntity metadataApiEntity) {
        this.publicId = metadataApiEntity.getPublicId();
        this.visible = metadataApiEntity.isVisible().orElse(null);

        metadataApiEntity.getCompetenceAims().ifPresent(competenceAims -> {
            grepCodes = new HashSet<>();
            competenceAims.stream().map(MetadataApiEntity.CompetenceAim::getCode).forEach(this::addGrepCode);
        });
        customFields = metadataApiEntity.getCustomFields().map(HashMap::new).orElse(null);
    }

    public static MetadataDto of(MetadataDto metadataDto) {
        final var newMetadataDto = new MetadataDto();
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

    public void setGrepCodes(Set<String> competenceAims) {
        this.grepCodes = competenceAims;
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
