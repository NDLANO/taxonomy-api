/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.service.UpdatableDto;

import java.net.URI;
import java.util.Optional;

public class SubjectPostPut implements UpdatableDto<Node> {
    @JsonProperty
    @Schema(description = "If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:subject:1")
    public URI id;

    @JsonProperty
    @Schema(description = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The name of the subject", example = "Mathematics")
    public String name;

    @Override
    public Optional<URI> getId() {
        return Optional.ofNullable(id);
    }

    @Override
    public void apply(Node subject) {
        if (getId().isPresent())
            subject.setPublicId(id);
        subject.setContext(true);
        subject.setRoot(true);
        subject.setName(name);
        subject.setContentUri(contentUri);
    }
}
