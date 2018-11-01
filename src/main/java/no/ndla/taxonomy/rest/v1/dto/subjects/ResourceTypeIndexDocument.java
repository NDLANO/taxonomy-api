package no.ndla.taxonomy.rest.v1.dto.subjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;

/**
 *
 */
@ApiModel("SubjectResourceTypeIndexDocument")
public class ResourceTypeIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Resource type id", example = "urn:resourcetype:12")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "Resource type name", example = "Assignment")
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
}
