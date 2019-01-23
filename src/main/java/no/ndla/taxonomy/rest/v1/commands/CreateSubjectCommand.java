package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Subject;

import java.net.URI;

public class CreateSubjectCommand extends CreateCommand<Subject> {
    @JsonProperty
    @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:subject:1")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(required = true, value = "The name of the subject", example = "Mathematics")
    public String name;

    @Override
    public URI getId() {
        return id;
    }

    @Override
    public void apply(Subject subject) {
        subject.setName(name);
        subject.setContentUri(contentUri);
    }
}
