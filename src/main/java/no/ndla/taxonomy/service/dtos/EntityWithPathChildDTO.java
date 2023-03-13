/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.service.TreeSorter;

import java.net.URI;

/**
 * Represents Node or Resource in child context
 */
public abstract class EntityWithPathChildDTO extends EntityWithPathDTO implements TreeSorter.Sortable {

    @Schema(description = "Parent id in the current context, null if none exists")
    @Deprecated
    public URI parent;

    @Schema(description = "Parent id in the current context, null if none exists")
    public URI parentId;

    @Schema(description = "The id of the node connection which causes this node to be included in the result set.", example = "urn:node-connection:1")
    public URI connectionId;

    @Schema(description = "Primary connection", example = "true")
    public boolean isPrimary;

    @Schema(description = "The order in which to sort the node within it's level.", example = "1")
    public int rank;

    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    public EntityWithPathChildDTO(Node node, NodeConnection nodeConnection, String language) {
        super(nodeConnection.getChild().orElseThrow(() -> new NotFoundException("Child was not found")), language);

        // This must be enabled when ed is updated to update metadata for connections.
        // this.metadata = new MetadataDto(nodeConnection.getMetadata());

        nodeConnection.getParent().ifPresent(parent -> this.parent = parent.getPublicId());
        nodeConnection.getParent().ifPresent(parent -> this.parentId = parent.getPublicId());

        this.rank = nodeConnection.getRank();
        this.connectionId = nodeConnection.getPublicId();
        this.isPrimary = nodeConnection.isPrimary().orElse(false);
        {
            final Relevance relevance = nodeConnection.getRelevance().orElse(null);
            this.relevanceId = relevance != null ? relevance.getPublicId() : null;
        }
    }

    @Override
    @JsonIgnore
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EntityWithPathChildDTO))
            return false;

        EntityWithPathChildDTO that = (EntityWithPathChildDTO) o;

        return getId().equals(that.getId());
    }

    public EntityWithPathChildDTO() {

    }

    public URI getParent() {
        return parent;
    }

    public URI getParentId() {
        return parentId;
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    @JsonProperty
    @Deprecated
    public boolean getPrimary() {
        return isPrimary;
    }

    public int getRank() {
        return rank;
    }

    public URI getRelevanceId() {
        return relevanceId;
    }

    @Override
    public int getSortableRank() {
        return rank;
    }

    @Override
    public URI getSortableId() {
        return getId();
    }

    @Override
    public URI getSortableParentId() {
        return parent;
    }
}
