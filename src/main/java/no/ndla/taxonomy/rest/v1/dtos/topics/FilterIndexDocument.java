package no.ndla.taxonomy.rest.v1.dtos.topics;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.FilterTranslation;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.TopicFilter;

import java.net.URI;
import java.util.Objects;

/**
 *
 */
@ApiModel("TopicFilterIndexDocument")
public class FilterIndexDocument {
    @JsonProperty
    @ApiModelProperty(example = "urn:filter:1")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the filter", example = "1T-YF")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "The id of the filter topic connection", example = "urn:topic-filter:1")
    public URI connectionId;

    @JsonProperty
    @ApiModelProperty(value = "The relevance of this topic according to the filter", example = "urn:relevance:core")
    public URI relevanceId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterIndexDocument that = (FilterIndexDocument) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(relevanceId, that.relevanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, relevanceId);
    }

    FilterIndexDocument() {

    }

    public FilterIndexDocument(TopicFilter topicFilter, String language) {
        final var filter = topicFilter.getFilter().orElseThrow(() -> new RuntimeException("Filter is not present in TopicFilter"));

        name = filter.getTranslation(language)
                .map(FilterTranslation::getName)
                .orElse(filter.getName());

        id = filter.getPublicId();
        connectionId = topicFilter.getPublicId();
        relevanceId = topicFilter.getRelevance().map(Relevance::getPublicId).orElse(null);
    }
}
