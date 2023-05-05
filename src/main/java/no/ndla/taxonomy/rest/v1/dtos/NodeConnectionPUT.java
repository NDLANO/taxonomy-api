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

public class NodeConnectionPUT {
    @JsonProperty
    @Schema(description = "Connection id", example = "urn:node-has-child:345")
    public URI id;

    @JsonProperty
    @Schema(description = "Backwards compatibility: Always true. Ignored on insert/update", example = "true")
    public boolean primary;

    @JsonProperty
    @Schema(description = "Order in which subtopic is sorted for the topic", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;
}
