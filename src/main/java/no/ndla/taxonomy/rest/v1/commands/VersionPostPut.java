/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.service.UpdatableDto;

import java.net.URI;
import java.util.Optional;

public class VersionPostPut implements UpdatableDto<Version> {
    @JsonProperty
    @Schema(description = "If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:subject:1")
    public URI id;

    @JsonProperty
    @Schema(description = "If specified, set the name to this value.", example = "Beta 2022")
    public String name;

    @JsonProperty
    @Schema(description = "If specified, set the locked property to this value.")
    public Boolean locked;

    @Override
    public Optional<URI> getId() {
        return Optional.ofNullable(id);
    }

    @Override
    public void apply(Version entity) {
        if (getId().isPresent())
            entity.setPublicId(id);
        if (name != null) {
            entity.setName(name);
        }
        if (locked != null) {
            entity.setLocked(locked);
        }
    }
}
