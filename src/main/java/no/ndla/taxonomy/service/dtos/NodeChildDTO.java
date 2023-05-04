/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.service.TreeSorter;

import java.net.URI;
import java.util.Optional;

/**
 * Represents Node or Resource in child context
 */
@Schema(name = "NodeChild")
public class NodeChildDTO extends NodeDTO implements TreeSorter.Sortable {

    @Schema(description = "Parent id in the current context, null if none exists")
    private URI parentId;

    @Schema(description = "The id of the node connection which causes this node to be included in the result set.", example = "urn:node-connection:1")
    private URI connectionId;

    @Schema(description = "Primary connection", example = "true")
    private boolean isPrimary;

    @Schema(description = "The order in which to sort the node within it's level.", example = "1")
    private int rank;

    @Schema(description = "Relevance id", example = "urn:relevance:core")
    private URI relevanceId;

    public NodeChildDTO(Optional<Node> root, NodeConnection nodeConnection, String language,
            Optional<Boolean> includeContexts) {
        super(root, nodeConnection.getChild().orElseThrow(() -> new NotFoundException("Child was not found")), language,
                Optional.empty(), includeContexts);

        // This must be enabled when ed is updated to update metadata for connections.
        // this.metadata = new MetadataDto(nodeConnection.getMetadata());

        nodeConnection.getParent().ifPresent(parent -> this.parentId = parent.getPublicId());

        this.rank = nodeConnection.getRank();
        this.connectionId = nodeConnection.getPublicId();
        this.isPrimary = nodeConnection.isPrimary().orElse(false);
        {
            final Relevance relevance = nodeConnection.getRelevance().orElse(null);
            this.relevanceId = relevance != null ? relevance.getPublicId() : URI.create("urn:relevance:core");
        }
    }

    /*
     * Special constructor used to get parents for resource/full
     */
    public NodeChildDTO(Node parent, NodeConnection nodeConnection, String language) {
        super(Optional.empty(), parent, language, Optional.empty(), Optional.of(false));

        this.rank = nodeConnection.getRank();
        this.connectionId = nodeConnection.getPublicId();
        this.isPrimary = nodeConnection.isPrimary().orElse(false);
        {
            final Relevance relevance = nodeConnection.getRelevance().orElse(null);
            this.relevanceId = relevance != null ? relevance.getPublicId() : URI.create("urn:relevance:core");
        }
    }

    @Override
    @JsonIgnore
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NodeChildDTO that))
            return false;

        return getId().equals(that.getId());
    }

    public NodeChildDTO() {

    }

    public URI getParent() {
        return parentId;
    }

    public URI getParentId() {
        return parentId;
    }

    public void setParentId(URI parentId) {
        this.parentId = parentId;
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(URI connectionId) {
        this.connectionId = connectionId;
    }

    @JsonProperty("isPrimary")
    public boolean isPrimary() {
        return isPrimary;
    }

    public boolean getPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public URI getRelevanceId() {
        return relevanceId;
    }

    public void setRelevanceId(URI relevanceId) {
        this.relevanceId = relevanceId;
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
        return parentId;
    }
}
