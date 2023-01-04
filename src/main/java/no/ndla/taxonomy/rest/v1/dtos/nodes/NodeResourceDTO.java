package no.ndla.taxonomy.rest.v1.dtos.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.net.URI;

public class NodeResourceDTO {

    @JsonProperty
    @Schema(description = "Node id", example = "urn:node:345")
    public URI nodeId;

    @JsonProperty
    @Schema(description = "Resource id", example = "urn:resource:345")
    public URI resourceId;

    @JsonProperty
    @Schema(description = "Node resource connection id", example = "urn:node-resource:123")
    public URI id;

    @JsonProperty
    @Schema(description = "Primary connection", example = "true")
    public boolean primary;

    @JsonProperty
    @Schema(description = "Order in which the resource is sorted for the node", example = "1")
    public int rank;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    @JsonProperty
    @Schema(description = "Metadata for entity. Read only.")
    private MetadataDto metadata;

    public NodeResourceDTO() {
    }

    public NodeResourceDTO(NodeConnection nodeResource) {
        id = nodeResource.getPublicId();
        nodeResource.getParent().ifPresent(node -> nodeId = node.getPublicId());
        nodeResource.getResource().ifPresent(resource -> resourceId = resource.getPublicId());
        primary = nodeResource.isPrimary().orElse(false);
        rank = nodeResource.getRank();
        relevanceId = nodeResource.getRelevance().map(DomainEntity::getPublicId).orElse(null);
        metadata = new MetadataDto(nodeResource.getMetadata());
    }
}
