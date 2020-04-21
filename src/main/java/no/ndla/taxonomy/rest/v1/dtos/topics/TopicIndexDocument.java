package no.ndla.taxonomy.rest.v1.dtos.topics;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.net.URI;

/**
 *
 */
@ApiModel("TopicIndexDocument")
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
    @ApiModelProperty(value = "The primary path for this topic", example = "/subject:1/topic:1")
    public String path;

    @ApiModelProperty(value = "Metadata object if includeMetadata has been set to true, otherwise null. Read only.")
    public MetadataDto metadata;
}
