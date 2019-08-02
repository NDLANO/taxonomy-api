package no.ndla.taxonomy.rest.v1.dtos.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.ResourceFilter;

import java.net.URI;
import java.util.Objects;

/**
 *
 */
@ApiModel("SubjectResourceFilterIndexDocument")
public class ResourceFilterIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Filter id", example = "urn:filter:12")
    public URI id;

    @JsonProperty
    @ApiModelProperty(required = true, value = "ID of the relevance the resource has in context of the filter", example = "urn:relevance:core")
    public URI relevanceId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceFilterIndexDocument that = (ResourceFilterIndexDocument) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(relevanceId, that.relevanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, relevanceId);
    }

    public ResourceFilterIndexDocument() {

    }

    public ResourceFilterIndexDocument(ResourceFilter resourceFilter) {
        this.id = resourceFilter.getFilter().getPublicId();
        this.relevanceId = resourceFilter.getRelevance().map(Relevance::getPublicId).orElse(null);
    }
}
