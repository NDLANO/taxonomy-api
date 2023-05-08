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

public class TopicResourcePUT {
    @JsonProperty
    @Schema(description = "Topic resource connection id", example = "urn:topic-has-resources:123")
    public URI id;

    @JsonProperty
    @Schema(description = "Primary connection", example = "true")
    public boolean primary;

    @JsonProperty
    @Schema(description = "Order in which the resource will be sorted for this topic.", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;
}
