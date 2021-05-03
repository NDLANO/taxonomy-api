package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceTranslation;
import no.ndla.taxonomy.service.InjectMetadata;
import no.ndla.taxonomy.service.MetadataIdField;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ApiModel("Resource")
public class ResourceDTO {
    @JsonProperty
    @ApiModelProperty(example = "urn:resource:345")
    @MetadataIdField
    private URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the resource", example = "Introduction to integration")
    private String name;

    @JsonProperty
    @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
            notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                    "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
    private URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "The path part of the url to this resource", example = "/subject:1/topic:1/resource:1")
    private String path;

    @ApiModelProperty(value = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    @JsonProperty
    @ApiModelProperty(value = "Resource type(s)", example = "[{\"id\": \"urn:resourcetype:1\",\"name\":\"lecture\"}]")
    private Set<ResourceTypeDTO> resourceTypes = new HashSet<>();

    @JsonProperty
    @ApiModelProperty(value = "Filters this resource is associated with, directly or by inheritance", example = "[{\"id\":\"urn:filter:1\", \"relevanceId\":\"urn:relevance:core\"}]")
    private Set<Object> filters = new HashSet<>();

    @JsonProperty
    @ApiModelProperty(value = "All paths that lead to this resource", example = "[\"/subject:1/topic:1/resource:1\", \"/subject:2/topic:3/resource:1\"]")
    private Set<String> paths;

    public ResourceDTO() {
    }

    protected void setMetadata(MetadataDto metadata) {
        this.metadata = metadata;
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

        this.path = resource.getPrimaryPath().orElse(null);
        this.paths = resource.getAllPaths();
    }

    public String getPath() {
        return path;
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

    public Set<Object> getFilters() {
        return filters;
    }

    public Set<String> getPaths() {
        return paths;
    }


}
