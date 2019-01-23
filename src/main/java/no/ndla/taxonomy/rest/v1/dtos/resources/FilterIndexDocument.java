package no.ndla.taxonomy.rest.v1.dtos.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.Objects;

/**
 *
 */
@ApiModel("ResourceFilterIndexDocument")
public class FilterIndexDocument {
    @JsonProperty
    @ApiModelProperty(example = "urn:filter:1")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the filter", example = "1T-YF")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "The id of the filter resource connection", example = "urn:resource-filter:1")
    public URI connectionId;

    @JsonProperty
    @ApiModelProperty(value = "The relevance of this resource according to the filter", example = "urn:relevance:1")
    public URI relevanceId;

    @JsonIgnore
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterIndexDocument that = (FilterIndexDocument) o;
        return Objects.equals(id, that.id);
    }

    @JsonIgnore
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
