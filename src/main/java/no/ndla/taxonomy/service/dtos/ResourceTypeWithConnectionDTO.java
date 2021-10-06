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
import no.ndla.taxonomy.domain.ResourceResourceType;

import java.net.URI;

@ApiModel("ResourceTypeWithConnection")
public class ResourceTypeWithConnectionDTO extends ResourceTypeDTO {
    @JsonProperty
    @ApiModelProperty(value = "The id of the resource resource type connection", example = "urn:resource-resourcetype:1")
    private URI connectionId;

    public ResourceTypeWithConnectionDTO() {
        super();
    }

    public ResourceTypeWithConnectionDTO(ResourceResourceType resourceResourceType, String languageCode) {
        super(resourceResourceType.getResourceType(), languageCode);

        this.connectionId = resourceResourceType.getPublicId();
    }

    public URI getConnectionId() {
        return connectionId;
    }
}
