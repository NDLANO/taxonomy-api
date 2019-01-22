package no.ndla.taxonomy.rest.v1.dto.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 *
 */
@ApiModel("ResourceWithPathsIndexDocument")
public class ResourceWithPathsIndexDocument extends ResourceIndexDocument {

    @JsonProperty
    @ApiModelProperty(value = "All paths that lead to this resource", example = "[\"/subject:1/topic:1/resource:1\", \"/subject:2/topic:3/resource:1\"}")
    public List<String> paths;
}
