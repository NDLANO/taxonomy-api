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

public class SubjectTopicPOST {
    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Subject id", example = "urn:subject:123")
    public URI subjectid;

    @JsonProperty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Topic id", example = "urn:topic:234")
    public URI topicid;

    @JsonProperty
    @Schema(description = "Backwards compatibility: Always true, ignored on insert/update.", example = "true")
    public boolean primary;

    @JsonProperty
    @Schema(description = "Order in which the topic should be sorted for the topic", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;
}
