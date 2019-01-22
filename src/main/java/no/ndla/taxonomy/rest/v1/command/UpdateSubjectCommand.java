package no.ndla.taxonomy.rest.v1.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Subject;

import java.net.URI;

public class UpdateSubjectCommand extends UpdateCommand<Subject> {
    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(required = true, value = "The name of the subject", example = "Mathematics")
    public String name;

    @Override
    public void apply(Subject subject) {
        subject.setName(name);
        subject.setContentUri(contentUri);
    }
}
