package no.ndla.taxonomy.rest.v1.dtos.subjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.ResourceResourceType;
import no.ndla.taxonomy.domain.ResourceTranslation;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.service.TopicTreeSorter;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
@ApiModel("SubjectResourceIndexDocument")
public class ResourceIndexDocument implements TopicTreeSorter.Sortable {
    @JsonProperty
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "Topic id", example = "urn:topic:12")
    public URI topicId;

    @JsonProperty
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "Resource type(s)", example = "[{\"id\": \"urn:resourcetype:1\",\"name\":\"lecture\"}]")
    public Set<ResourceTypeIndexDocument> resourceTypes = new HashSet<>();

    @JsonProperty
    @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
            notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                    "for the system, and <id> is the id of this content in that system.",
            example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "URL path for resource", example = "'/subject:1/topic:12/resource:12")
    public String path;

    @JsonProperty
    @ApiModelProperty(value = "The id of the topic-resource connection which causes this resource to be included in the result set.", example = "urn:topic-resource:1")
    public URI connectionId;

    @JsonProperty
    @ApiModelProperty(value = "Filters this resource is associated with, directly or by inheritance", example = "[{\"id\":\"urn:filter:1\", \"relevanceId\":\"urn:relevance:core\"}]")
    public Set<ResourceFilterIndexDocument> filters = new HashSet<>();

    @JsonProperty
    @ApiModelProperty(value = "Rank relative to parent", example = "1")
    public int rank;

    @JsonIgnore
    public int topicNumericId;

    public ResourceIndexDocument() {

    }

    public ResourceIndexDocument(TopicResource topicResource, String language) {
        topicResource.getTopic().ifPresent(topic -> {
            this.topicId = topic.getPublicId();
            this.topicNumericId = topic.getId();
        });

        topicResource.getResource().ifPresent(resource -> {
            this.id = resource.getPublicId();
            this.name = resource.getTranslation(language)
                    .map(ResourceTranslation::getName)
                    .orElse(resource.getName());

            this.resourceTypes = resource.getResourceResourceTypes()
                    .stream()
                    .map(ResourceResourceType::getResourceType)
                    .map(resourceType -> new ResourceTypeIndexDocument(resourceType, language))
                    .collect(Collectors.toSet());

            this.contentUri = resource.getContentUri();
            this.path = resource.getPrimaryPath().orElse(null);
            this.connectionId = topicResource.getPublicId();

            this.filters = resource.getResourceFilters()
                    .stream()
                    .map(ResourceFilterIndexDocument::new)
                    .collect(Collectors.toSet());
        });

        this.rank = topicResource.getRank();
    }

    @Override
    public int getSortableRank() {
        return rank;
    }

    @Override
    public URI getSortableId() {
        return id;
    }

    @Override
    public URI getSortableParentId() {
        return topicId;
    }
}
