/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;

import java.net.URI;

@ApiModel("ResourceWithTopicConnection")
public class ResourceWithNodeConnectionDTO extends ResourceDTO {
    @ApiParam
    private URI parentId;

    @ApiParam
    private URI connectionId;

    @ApiParam
    private int rank;

    @ApiParam
    private boolean primary;

    @ApiParam
    public URI relevanceId;

    public ResourceWithNodeConnectionDTO() {

    }

    public ResourceWithNodeConnectionDTO(NodeConnection nodeResource, String language) {
        super(nodeResource.getResource().orElseThrow(() -> new NotFoundException("NodeResource was not found")),
                language);

        this.parentId = nodeResource.getParent().map(Node::getPublicId).orElse(null);

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
