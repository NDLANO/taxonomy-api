package no.ndla.taxonomy.rest.v1.dtos.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.Objects;

/**
 *
 */
@ApiModel("SubjectTopicFilterIndexDocument")
public class TopicFilterIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Filter id", example = "urn:filter:12")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "Filter name", example = "VG 1")
    public String name;

    @JsonProperty
    @ApiModelProperty(required = true, value = "ID of the relevance the resource has in context of the filter", example = "urn:relevance:core")
    public URI relevanceId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicFilterIndexDocument that = (TopicFilterIndexDocument) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id.toASCIIString());
    }
}
