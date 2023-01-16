/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class NodeConnectionPage {
    @JsonProperty
    @Schema(description = "Total number of elements")
    public long totalCount;

    @JsonProperty
    @Schema(description = "Page containing results")
    public List<ParentChildIndexDocument> results;

    NodeConnectionPage() {
    }

    public NodeConnectionPage(long totalCount, List<ParentChildIndexDocument> results) {
        this.totalCount = totalCount;
        this.results = results;
    }
}
