package no.ndla.taxonomy.rest.v1.dtos.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.FilterTranslation;
import no.ndla.taxonomy.domain.TopicFilter;

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

    public TopicFilterIndexDocument() {

    }

    public TopicFilterIndexDocument(TopicFilter topicFilter, String language) {
        final var filter = topicFilter.getFilter().orElseThrow(() -> new RuntimeException("No filter found on TopicFilter"));
        final var relevance = topicFilter.getRelevance().orElseThrow(() -> new RuntimeException("No relevance found on TopicFilter"));

        this.id = filter.getPublicId();
        this.name = filter.getTranslation(language).map(FilterTranslation::getName).orElse(filter.getName());
        this.relevanceId = relevance.getPublicId();
    }
}
