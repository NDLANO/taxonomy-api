package no.ndla.taxonomy.rest.v1.dto.queries;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.Set;


@ApiModel("QueryTopicIndexDocument")
public class TopicIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the topic", example = "Trigonometry")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "The path part of the url for this topic", example = "/subject:1/topic:1")
    public String path;

    @JsonProperty
    @ApiModelProperty(value = "All paths that lead to this topic", example = "[\"/subject:1/topic:12/topic:12\", \"/subject:2/topic:13/topic:12\"]")
    public Set<String> paths;

}
