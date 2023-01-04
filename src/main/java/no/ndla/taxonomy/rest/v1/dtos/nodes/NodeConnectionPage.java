/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class NodeConnectionPage {
    @JsonProperty
    @ApiModelProperty(value = "Total number of elements")
    public long totalCount;

    @JsonProperty
    @ApiModelProperty(value = "Page containing results")
    public List<ParentChildIndexDocument> results;

    NodeConnectionPage() {
    }

    public NodeConnectionPage(long totalCount, List<ParentChildIndexDocument> results) {
        this.totalCount = totalCount;
        this.results = results;
    }
}
