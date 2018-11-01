package no.ndla.taxonomy.rest.v1.dto.topics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@ApiModel("TopicResourceIndexDocument")
public class ResourceIndexDocument {
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
    @ApiModelProperty(value = "Resource type(s)", example = "[{id = 'urn:resourcetype:1', name = 'lecture'}]")
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
    @ApiModelProperty(value = "The id of the topic-resource connection which causes this resource to be included in the result set.", example = "urn:topic-resource:1")
    public URI connectionId;

    @JsonProperty
    @ApiModelProperty(value = "The order in which to sort the topic within it's level.", example = "1")
    public int rank;

    @JsonProperty
    @ApiModelProperty(value = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
    public Boolean isPrimary;

    @JsonIgnore
    public int topicNumericId;
}
