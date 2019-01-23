package no.ndla.taxonomy.rest.v1.dtos.topics;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;

/**
 *
 */
@ApiModel("SubTopicIndexDocument")
public class SubTopicIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the subtopic", example = "Trigonometry")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
    public Boolean isPrimary;

}
