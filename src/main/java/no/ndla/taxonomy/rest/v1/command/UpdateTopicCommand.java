package no.ndla.taxonomy.rest.v1.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Topic;

import java.net.URI;

public class UpdateTopicCommand extends UpdateCommand<Topic> {
    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(required = true, value = "The name of the topic", example = "Trigonometry")
    public String name;

    @Override
    public void apply(Topic topic) {
        topic.setName(name);
        topic.setContentUri(contentUri);
    }
}
