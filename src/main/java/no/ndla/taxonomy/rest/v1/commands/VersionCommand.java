/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.service.UpdatableDto;

import java.net.URI;
import java.util.Optional;

public class VersionCommand implements UpdatableDto<Version> {
    @JsonProperty
    @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:subject:1")
    public URI id;

    @Override
    public Optional<URI> getId() {
        return Optional.ofNullable(id);
    }

    @Override
    public void apply(Version entity) {
        if (getId().isPresent())
            entity.setPublicId(id);
    }
}
