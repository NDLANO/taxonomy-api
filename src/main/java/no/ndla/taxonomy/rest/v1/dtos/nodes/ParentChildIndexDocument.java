/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.net.URI;

public class ParentChildIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Parent id", example = "urn:topic:234")
    public URI parentId;

    @JsonProperty
    @ApiModelProperty(value = "Child id", example = "urn:topic:234")
    public URI childId;

    @JsonProperty
    @ApiModelProperty(value = "Connection id", example = "urn:topic-has-subtopics:345")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "Backwards compatibility: Always true. Ignored on insert/update", example = "true")
    public boolean primary;

    @JsonProperty
    @ApiModelProperty(value = "Order in which subtopic is sorted for the topic", example = "1")
    public int rank;

    @JsonProperty
    @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    @JsonProperty
    @ApiModelProperty(value = "Metadata for entity. Read only.")
    private MetadataDto metadata;

    ParentChildIndexDocument() {
    }

    public ParentChildIndexDocument(NodeConnection nodeConnection) {
        id = nodeConnection.getPublicId();
        nodeConnection.getParent().ifPresent(topic -> parentId = topic.getPublicId());
        nodeConnection.getChild().ifPresent(subtopic -> childId = subtopic.getPublicId());
        relevanceId = nodeConnection.getRelevance().map(Relevance::getPublicId).orElse(null);
        primary = true;
        rank = nodeConnection.getRank();
        metadata = new MetadataDto(nodeConnection.getMetadata());
    }
}
