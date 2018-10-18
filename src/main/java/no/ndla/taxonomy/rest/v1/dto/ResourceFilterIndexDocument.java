package no.ndla.taxonomy.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;

/**
 *
 */
@ApiModel("SubjectResourceFilterIndexDocument")
public class ResourceFilterIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Filter id", example = "urn:filter:12")
    public URI id;

    @JsonProperty
    @ApiModelProperty(required = true, value = "ID of the relevance the resource has in context of the filter", example = "urn:relevance:core")
    public URI relevanceId;
}
