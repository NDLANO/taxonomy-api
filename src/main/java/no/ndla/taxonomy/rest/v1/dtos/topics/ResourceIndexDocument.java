package no.ndla.taxonomy.rest.v1.dtos.topics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.ResourceResourceType;
import no.ndla.taxonomy.domain.ResourceTranslation;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.service.MetadataWrappedEntity;
import no.ndla.taxonomy.service.TopicTreeSorter;
import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
@ApiModel("TopicResourceIndexDocument")
public class ResourceIndexDocument implements TopicTreeSorter.Sortable {
    @JsonProperty
    @ApiModelProperty(value = "Topic id", example = "urn:topic:123")
    public URI topicId;

    @JsonProperty
    @ApiModelProperty(value = "Resource id", example = "urn:resource:12")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "Resource name", example = "Basic physics")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "Resource type(s)", example = "[{\"id\":\"urn:resourcetype:1\", \"name\":\"lecture\"}]")
    public Set<ResourceTypeIndexDocument> resourceTypes = new HashSet<>();

    @JsonProperty
    @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
            notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                    "for the system, and <id> is the id of this content in that system.",
            example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "URL path for resource", example = "'/subject:1/topic:12/resource:12'")
    public String path;

    @JsonProperty
    @ApiModelProperty(value = "Paths containing the given topic that lead to this resource", example = "[\"/subject:1/topic:12/resource:1\", \"/subject:2/topic:12/resource:1\"]")
    public Set<String> paths;

    @JsonProperty
    @ApiModelProperty(value = "The id of the topic-resource connection which causes this resource to be included in the result set.", example = "urn:topic-resource:1")
    public URI connectionId;

    @JsonProperty
    @ApiModelProperty(value = "The order in which to sort the topic within it's level.", example = "1")
    public int rank;

    @JsonProperty
    @ApiModelProperty(value = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
    public Boolean isPrimary;

    @ApiModelProperty(value = "Metadata object if includeMetadata has been set to true. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public MetadataDto metadata;

    public URI getId() {
        return id;
    }

    public ResourceIndexDocument() {

    }

    public ResourceIndexDocument(MetadataWrappedEntity<TopicResource> wrappedTopicResource, String language) {
        this(wrappedTopicResource.getEntity(), language);

        wrappedTopicResource.getMetadata().ifPresent(metadataDto -> this.metadata = metadataDto);
    }

    public ResourceIndexDocument(TopicResource topicResource, String language) {
        topicResource.getTopic().ifPresent(topic -> this.topicId = topic.getPublicId());

        topicResource.getResource().ifPresent(resource -> {
            this.id = resource.getPublicId();

            this.name = resource.getTranslation(language)
                    .map(ResourceTranslation::getName)
                    .orElse(resource.getName());

            this.resourceTypes = resource.getResourceResourceTypes().stream()
                    .map(ResourceResourceType::getResourceType)
                    .map(resourceType -> new ResourceTypeIndexDocument(resourceType, language))
                    .collect(Collectors.toSet());

            this.contentUri = resource.getContentUri();
            this.path = resource.getPrimaryPath().orElse(null);
            this.paths = resource.getAllPaths();
        });

        this.connectionId = topicResource.getPublicId();
        this.rank = topicResource.getRank();
        this.isPrimary = topicResource.isPrimary().orElseThrow();
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
