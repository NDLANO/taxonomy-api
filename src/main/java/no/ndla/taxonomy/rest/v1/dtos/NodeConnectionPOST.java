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

public class NodeConnectionPOST {
    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = "Parent id", example = "urn:topic:234")
    public URI parentId;

    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Child id", example = "urn:topic:234")
    public URI childId;

    @JsonProperty
    @Schema(description = "Backwards compatibility: Always true. Ignored on insert/update", example = "true")
    public boolean primary = true;

    @JsonProperty
    @Schema(description = "Order in which to sort the child for the parent", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;
}
