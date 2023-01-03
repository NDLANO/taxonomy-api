/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeResource;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;

import java.net.URI;

@Schema(name = "ResourceWithTopicConnection")
public class ResourceWithNodeConnectionDTO extends ResourceDTO {
    @Schema
    private URI parentId;

    @Schema
    private URI connectionId;

    @Schema
    private int rank;

    @Schema
    private boolean primary;

    @Schema
    public URI relevanceId;

    public ResourceWithNodeConnectionDTO() {

    }

    public ResourceWithNodeConnectionDTO(NodeResource nodeResource, String language) {
        super(nodeResource.getResource().orElseThrow(() -> new NotFoundException("NodeResource was not found")),
                language);

        this.parentId = nodeResource.getNode().map(Node::getPublicId).orElse(null);

        this.connectionId = nodeResource.getPublicId();
        this.rank = nodeResource.getRank();
        this.primary = nodeResource.isPrimary().orElse(false);
        {
            final Relevance relevance = nodeResource.getRelevance().orElse(null);
            this.relevanceId = relevance != null ? relevance.getPublicId() : null;
        }
    }

    public URI getParentId() {
        return parentId;
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public int getRank() {
        return rank;
    }

    public boolean isPrimary() {
        return primary;
    }
}
