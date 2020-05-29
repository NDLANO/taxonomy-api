package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceTranslation;
import no.ndla.taxonomy.service.MetadataWrappedEntity;

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApiModel("Resource")
public class ResourceDTO {
    @JsonProperty
    @ApiModelProperty(example = "urn:resource:345")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the resource", example = "Introduction to integration")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
            notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                    "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "The path part of the url to this resource", example = "/subject:1/topic:1/resource:1")
    public String path;

    @ApiModelProperty(value = "Metadata object if includeMetadata has been set to true. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public MetadataDto metadata;

    @JsonProperty
    @ApiModelProperty(value = "Resource type(s)", example = "[{\"id\": \"urn:resourcetype:1\",\"name\":\"lecture\"}]")
    public Set<ResourceTypeDTO> resourceTypes = new HashSet<>();

    @JsonProperty
    @ApiModelProperty(value = "Filters this resource is associated with, directly or by inheritance", example = "[{\"id\":\"urn:filter:1\", \"relevanceId\":\"urn:relevance:core\"}]")
    public Set<FilterWithConnectionDTO> filters = new HashSet<>();

    @JsonProperty
    @ApiModelProperty(value = "All paths that lead to this resource", example = "[\"/subject:1/topic:1/resource:1\", \"/subject:2/topic:3/resource:1\"]")
    public Set<String> paths;

    public ResourceDTO() {
    }

    public ResourceDTO(MetadataWrappedEntity<Resource> wrappedResource, String languageCode) {
        this(wrappedResource.getEntity(), languageCode);

        wrappedResource.getMetadata().ifPresent(metadataDto -> this.metadata = metadataDto);
    }

    public ResourceDTO(Resource resource, String languageCode) {
        this.id = resource.getPublicId();
        this.contentUri = resource.getContentUri();
        this.name = resource
                .getTranslation(languageCode)
                .map(ResourceTranslation::getName)
                .orElse(resource.getName());

        this.resourceTypes = resource.getResourceResourceTypes()
                .stream()
                .map(resourceType -> new ResourceTypeWithConnectionDTO(resourceType, languageCode))
                .collect(Collectors.toSet());

        this.filters = resource.getResourceFilters()
                .stream()
                .map(resourceFilter -> new FilterWithConnectionDTO(resourceFilter, languageCode))
                .collect(Collectors.toSet());

        this.path = resource.getPrimaryPath().orElse(null);
        this.paths = resource.getAllPaths();
    }

    Optional<String> getPath() {
        return Optional.ofNullable(path);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public URI getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public MetadataDto getMetadata() {
        return metadata;
    }

    public Set<ResourceTypeDTO> getResourceTypes() {
        return resourceTypes;
    }

    public Set<FilterWithConnectionDTO> getFilters() {
        return filters;
    }

    public Set<String> getPaths() {
        return paths;
    }
}
