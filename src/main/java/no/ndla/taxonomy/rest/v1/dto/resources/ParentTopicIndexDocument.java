package no.ndla.taxonomy.rest.v1.dto.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.Objects;

/**
 *
 */
@ApiModel("ResourceParentTopicIndexDocument")
public class ParentTopicIndexDocument {
    @JsonProperty
    public URI id;

    @JsonProperty
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "Primary connection", example = "true")
    public boolean isPrimary;

    @JsonProperty
    public URI connectionId;

    @Override
    @JsonIgnore
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParentTopicIndexDocument that = (ParentTopicIndexDocument) o;
        return Objects.equals(id, that.id);
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return id.hashCode();
    }
}
