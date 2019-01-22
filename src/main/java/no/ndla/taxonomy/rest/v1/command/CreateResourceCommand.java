package no.ndla.taxonomy.rest.v1.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.rest.v1.command.CreateCommand;

import java.net.URI;

public class CreateResourceCommand extends CreateCommand<Resource> {
    @JsonProperty
    @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:resource: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:resource:2")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "The ID of this resource in the system where the content is stored.",
            notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                    "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(required = true, value = "The name of the resource", example = "Introduction to integration")
    public String name;

    @Override
    public URI getId() {
        return id;
    }

    @Override
    public void apply(Resource entity) {
        entity.setName(name);
        entity.setContentUri(contentUri);
    }
}
