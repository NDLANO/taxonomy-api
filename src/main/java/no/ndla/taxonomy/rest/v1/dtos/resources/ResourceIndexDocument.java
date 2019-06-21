package no.ndla.taxonomy.rest.v1.dtos.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceTranslation;

import java.net.URI;
import java.util.Optional;

/**
 *
 */
@ApiModel("ResourceIndexDocument")
public class ResourceIndexDocument {
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

    public ResourceIndexDocument() {
    }

    public ResourceIndexDocument(Resource resource) {
        this.id = resource.getPublicId();
        this.contentUri = resource.getContentUri();
        this.name = resource.getName();
    }

    public ResourceIndexDocument(Resource resource, String languageCode) {
        this.id = resource.getPublicId();
        this.contentUri = resource.getContentUri();
        this.name = resource
                .getTranslation(languageCode)
                .map(ResourceTranslation::getName)
                .orElse(resource.getName());
    }

    Optional<String> getPath() {
        return Optional.ofNullable(path);
    }

    public void setPath(String path) {
        this.path = path;
    }
}
