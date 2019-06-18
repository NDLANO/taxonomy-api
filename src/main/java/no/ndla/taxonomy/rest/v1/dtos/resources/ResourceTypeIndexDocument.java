package no.ndla.taxonomy.rest.v1.dtos.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.ResourceResourceType;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.ResourceTypeTranslation;

import java.net.URI;
import java.util.Objects;

/**
 *
 */
@ApiModel("ResourceTypeIndexDocument")
public class ResourceTypeIndexDocument {
    @JsonProperty
    @ApiModelProperty(example = "urn:resourcetype:2")
    public URI id;

    @JsonProperty
    @ApiModelProperty(example = "urn:resourcetype:1")
    public URI parentId;

    @JsonProperty
    @ApiModelProperty(value = "The name of the resource type", example = "Lecture")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "The id of the resource resource type connection", example = "urn:resource-resourcetype:1")
    public URI connectionId;

    @Override
    @JsonIgnore
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceTypeIndexDocument that = (ResourceTypeIndexDocument) o;
        return Objects.equals(id, that.id);
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return id.hashCode();
    }

    public ResourceTypeIndexDocument() {

    }

    public ResourceTypeIndexDocument(ResourceResourceType resourceResourceType, String languageCode) {
        this.id = resourceResourceType.getResourceType().getPublicId();

        resourceResourceType
                .getResourceType()
                .getParent()
                .map(ResourceType::getPublicId)
                .ifPresent(publicId -> this.parentId = publicId);

        this.name = resourceResourceType
                .getResourceType()
                .getTranslation(languageCode)
                .map(ResourceTypeTranslation::getName)
                .orElse(resourceResourceType.getResourceType().getName());

        this.connectionId = resourceResourceType.getPublicId();
    }
}
