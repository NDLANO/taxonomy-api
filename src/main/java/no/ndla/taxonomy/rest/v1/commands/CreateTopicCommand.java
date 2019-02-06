package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Topic;

import java.net.URI;

public class CreateTopicCommand extends CreateCommand<Topic> {
    @JsonProperty
    @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:topic:1")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(required = true, value = "The name of the topic", example = "Trigonometry")
    public String name;

    @Override
    public URI getId() {
        return id;
    }

    @Override
    public void apply(Topic topic) {
        topic.setName(name);
        topic.setContentUri(contentUri);
    }
}
