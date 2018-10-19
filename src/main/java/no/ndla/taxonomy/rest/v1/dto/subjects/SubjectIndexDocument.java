package no.ndla.taxonomy.rest.v1.dto.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;

/**
 *
 */
@ApiModel("SubjectIndexDocument")
public class SubjectIndexDocument {
    @JsonProperty
    @ApiModelProperty(example = "urn:subject:1")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "The name of the subject", example = "Mathematics")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "The path part of the url to this subject.", example = "/subject:1")
    public String path;
}
