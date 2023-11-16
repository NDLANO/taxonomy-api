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
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.service.dtos.MetadataDTO;

@Schema(name = "NodeConnection")
public class NodeConnectionDTO {
    @JsonProperty
    @Schema(description = "Parent id", example = "urn:topic:234")
    public URI parentId;

    @JsonProperty
    @Schema(description = "Child id", example = "urn:topic:234")
    public URI childId;

    @JsonProperty
    @Schema(description = "Connection id", example = "urn:topic-has-subtopics:345")
    public URI id;

    @JsonProperty
    @Schema(description = "Is this connection primary", example = "true")
    public boolean primary;

    @JsonProperty
    @Schema(description = "Order in which subtopic is sorted for the topic", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId;

    @JsonProperty
    @Schema(description = "Metadata for entity. Read only.")
    public MetadataDTO metadata;

    NodeConnectionDTO() {}

    public NodeConnectionDTO(NodeConnection nodeConnection) {
        id = nodeConnection.getPublicId();
        nodeConnection.getParent().ifPresent(topic -> parentId = topic.getPublicId());
        nodeConnection.getChild().ifPresent(subtopic -> childId = subtopic.getPublicId());
        relevanceId = nodeConnection.getRelevance().map(Relevance::getPublicId);
        primary = true;
        rank = nodeConnection.getRank();
        metadata = new MetadataDTO(nodeConnection.getMetadata());
    }
}
