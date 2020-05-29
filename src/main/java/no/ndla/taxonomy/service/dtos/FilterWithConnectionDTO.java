package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.ResourceFilter;
import no.ndla.taxonomy.domain.TopicFilter;

import java.net.URI;

@ApiModel("FilterWithConnection")
public class FilterWithConnectionDTO extends FilterDTO {
    @JsonProperty
    @ApiModelProperty(value = "The id of the filter resource connection", example = "urn:resource-filter:1")
    private URI connectionId;

    @JsonProperty
    @ApiModelProperty(value = "The relevance of this resource according to the filter", example = "urn:relevance:1")
    private URI relevanceId;

    public FilterWithConnectionDTO() {

    }

    public FilterWithConnectionDTO(ResourceFilter resourceFilter, String languageCode) {
        super(resourceFilter.getFilter(), languageCode);

        this.connectionId = resourceFilter.getPublicId();

        resourceFilter
                .getRelevance()
                .map(Relevance::getPublicId)
                .ifPresent(publicId -> this.relevanceId = publicId);
    }

    public FilterWithConnectionDTO(TopicFilter topicFilter, String languageCode) {
        super(topicFilter.getFilter().orElseThrow(), languageCode);

        this.connectionId = topicFilter.getPublicId();
        this.relevanceId = topicFilter.getRelevance().orElseThrow().getPublicId();
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public URI getRelevanceId() {
        return relevanceId;
    }
}
