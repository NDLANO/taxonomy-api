/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
public class NodeConnection extends DomainEntity implements EntityWithMetadata, EntityWithPathConnection,
        Comparable<NodeConnection>, SortableResourceConnection<Node> {
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Node parent;

    @ManyToOne
    @JoinColumn(name = "child_id")
    private Node child;

    @Column(name = "rank")
    private int rank;

    @Column(name = "is_primary")
    private boolean isPrimary;

    @ManyToOne
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "metadata_id")
    private Metadata metadata = new Metadata();

    private NodeConnection() {
        setPublicId(URI.create("urn:node-connection:" + UUID.randomUUID()));
    }

    public NodeConnection(NodeConnection nodeConnection) {
        this.rank = nodeConnection.rank;
        this.relevance = nodeConnection.relevance;
        this.parent = nodeConnection.parent;
        this.child = nodeConnection.child;
        setPublicId(nodeConnection.getPublicId());
        setPrimary(nodeConnection.isPrimary().orElse(false));
        setMetadata(new Metadata(nodeConnection.getMetadata()));
    }

    public static NodeConnection create(Node parent, Node child) {
        return NodeConnection.create(parent, child, true);
    }

    public static NodeConnection create(Node parent, Node child, boolean isPrimary) {
        if (child == null || parent == null) {
            throw new NullPointerException("Both parent and child must be present.");
        }

        final var nodeConnection = new NodeConnection();
        nodeConnection.parent = parent;
        nodeConnection.child = child;
        nodeConnection.isPrimary = isPrimary;

        parent.addChildConnection(nodeConnection);
        child.addParentConnection(nodeConnection);

        return nodeConnection;
    }

    public void disassociate() {
        final var parent = this.parent;
        final var child = this.child;

        this.parent = null;
        this.child = null;

        if (parent != null) {
            parent.removeChildConnection(this);
        }
        if (child != null) {
            child.removeParentConnection(this);
        }
    }

    @Override
    public Optional<Node> getResource() {
        var child = getChild();
        var isResource = child.map(c -> c.getNodeType() == NodeType.RESOURCE).orElse(false);

        if (!isResource)
            throw new IllegalStateException("Tried to getResource on a nodeConnection connected to a non-resource");

        return child;
    }

    public Optional<Node> getParent() {
        return Optional.ofNullable(parent);
    }

    public Optional<Node> getChild() {
        return Optional.ofNullable(child);
    }

    // only for publishing
    public void setParent(Node parent) {
        this.parent = parent;
    }

    // only for publishing
    public void setChild(Node child) {
        this.child = child;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public Optional<EntityWithPath> getConnectedParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public Optional<EntityWithPath> getConnectedChild() {
        return Optional.ofNullable(child);
    }

    @PreRemove
    public void preRemove() {
        disassociate();
    }

    @Override
    public Optional<Boolean> isPrimary() {
        return Optional.of(this.isPrimary);
    }

    @Override
    public void setPrimary(boolean isPrimary) {
        var childType = this.child.getNodeType();
        if (childType != NodeType.RESOURCE && !isPrimary) {
            throw new UnsupportedOperationException(
                    "NodeConnection with child of type '" + childType.toString() + "' can not be non-primary");
        }

        this.isPrimary = isPrimary;
    }

    @Override
    public Optional<Relevance> getRelevance() {
        return Optional.ofNullable(relevance);
    }

    @Override
    public void setRelevance(Relevance relevance) {
        this.relevance = relevance;
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public int compareTo(NodeConnection o) {
        return getPublicId().compareTo(o.getPublicId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NodeConnection that = (NodeConnection) o;
        return rank == that.rank && parent.getPublicId().equals(that.parent.getPublicId())
                && child.getPublicId().equals(that.child.getPublicId()) && Objects.equals(relevance, that.relevance)
                && metadata.equals(that.metadata);
    }
}
