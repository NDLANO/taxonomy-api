/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.NodeResource;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;

import java.net.URI;

@ApiModel("NodeWithResourceConnection")
public class NodeWithResourceConnectionDTO extends NodeDTO {
    @ApiParam
    private URI connectionId;

    @ApiParam
    @JsonProperty("isPrimary")
    private boolean isPrimary;

    @ApiParam
    private int rank;

    @ApiParam
    private URI relevanceId;

    @ApiModelProperty(value = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    public NodeWithResourceConnectionDTO(NodeResource nodeResource, String language) {
        super(nodeResource.getNode().orElseThrow(() -> new NotFoundException("Node was not found")), language);

        this.isPrimary = nodeResource.isPrimary().orElse(false);
        this.connectionId = nodeResource.getPublicId();
        this.rank = nodeResource.getRank();
        this.relevanceId = nodeResource.getRelevance().map(Relevance::getPublicId)
                .orElse(URI.create("urn:relevance:core"));
        this.metadata = new MetadataDto(nodeResource.getMetadata());
    }

    public NodeWithResourceConnectionDTO() {
        super();
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public URI getRelevanceId() {
        return relevanceId;
    }

    @Override
    public MetadataDto getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (NodeWithResourceConnectionDTO) obj;
        return other.connectionId.equals(this.connectionId);
    }

    @Override
    public int hashCode() {
        return connectionId.hashCode();
    }
}
