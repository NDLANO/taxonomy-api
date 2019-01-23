package no.ndla.taxonomy.rest.v1.dtos.queries;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.Set;

@ApiModel("QueryResourceIndexDocument")
public class ResourceIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Resource id", example = "urn:resource:12")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "Resource name", example = "Basic physics")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "Resource type(s)", example = "[{\"id\":\"urn:resourcetype:learningPath\", \"name\":\"Learning path\"}]")
    public Set<ResourceTypeIndexDocument> resourceTypes;

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
    @ApiModelProperty(value = "All paths leading to this resource", example = "[\"/subject:1/topic:12/resource:12\", \"/subject:2/topic:13/resource:12\"]")
    public Set<String> paths;

}
