package no.ndla.taxonomy.rest.v1.dtos.queries;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.ResourceTypeTranslation;

import java.net.URI;

@ApiModel("QueryResourceTypeIndexDocument")
public class ResourceTypeIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Resource type id", example = "urn:resourcetype:learningPath")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "Resource type name", example = "Learning path")
    public String name;

    @Override
    @JsonIgnore
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceTypeIndexDocument)) return false;
        ResourceTypeIndexDocument that = (ResourceTypeIndexDocument) o;
        return id.equals(that.id);
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return id.hashCode();
    }

    public ResourceTypeIndexDocument() {

    }

    public ResourceTypeIndexDocument(ResourceType resourceType, String languageCode) {
        this.id = resourceType.getPublicId();
        this.name = resourceType
                .getTranslation(languageCode)
                .map(ResourceTypeTranslation::getName)
                .orElse(resourceType.getName());
    }
}