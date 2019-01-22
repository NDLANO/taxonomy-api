package no.ndla.taxonomy.rest.v1.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Resource;

import java.net.URI;


public class UpdateResourceCommand extends UpdateCommand<Resource> {
    @JsonProperty
    @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
            notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                    "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "The name of the resource", example = "Introduction to integration")
    public String name;

    @Override
    public void apply(Resource resource) {
        resource.setName(name);
        resource.setContentUri(contentUri);
    }
}
