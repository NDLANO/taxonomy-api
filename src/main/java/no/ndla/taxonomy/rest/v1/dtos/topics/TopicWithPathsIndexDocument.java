package no.ndla.taxonomy.rest.v1.dtos.topics;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel("TopicWithPathsIndexDocument")
public class TopicWithPathsIndexDocument extends TopicIndexDocument {

    @JsonProperty
    @ApiModelProperty(value = "All paths that lead to this topic", example = "[\"/subject:1/topic:1\", \"/subject:2/topic:1\"]")
    public List<String> paths;

}
