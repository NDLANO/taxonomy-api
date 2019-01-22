package no.ndla.taxonomy.rest.v1.dto.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.rest.v1.Resources;

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
}
