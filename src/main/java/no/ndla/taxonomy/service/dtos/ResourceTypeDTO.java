/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.ResourceTypeTranslation;

import java.net.URI;

@ApiModel("ResourceType")
public class ResourceTypeDTO implements Comparable<ResourceTypeDTO> {
    @JsonProperty
    @ApiModelProperty(example = "urn:resourcetype:2")
    private URI id;

    @JsonProperty
    @ApiModelProperty(example = "urn:resourcetype:1")
    private URI parentId;

    @JsonProperty
    @ApiModelProperty(value = "The name of the resource type", example = "Lecture")
    private String name;

    public ResourceTypeDTO() {
    }

    public ResourceTypeDTO(ResourceType resourceType, String languageCode) {
        this.id = resourceType.getPublicId();

        resourceType.getParent().map(ResourceType::getPublicId).ifPresent(publicId -> this.parentId = publicId);

        this.name = resourceType.getTranslation(languageCode).map(ResourceTypeTranslation::getName)
                .orElse(resourceType.getName());
    }

    public URI getId() {
        return id;
    }

    public URI getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(ResourceTypeDTO o) {
        // We want to sort resourceTypes without parents first when sorting
        if (this.parentId == null && o.parentId != null)
            return 1;
        return -1;
    }
}
