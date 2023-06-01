/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.Optional;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.service.UpdatableDto;

public class RelevancePUT implements UpdatableDto<Relevance> {
    @JsonProperty
    @Schema(
            description =
                    "If specified, set the id to this value. Must start with urn:relevance: and be a valid URI. If ommitted, an id will be assigned automatically. Ignored on update",
            example = "urn:relevance:supplementary")
    public URI id;

    @JsonProperty
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "The name of the relevance",
            example = "Supplementary")
    public String name;

    @Override
    public Optional<URI> getId() {
        return Optional.ofNullable(id);
    }

    @Override
    public void apply(Relevance entity) {
        entity.setName(name);
    }
}
