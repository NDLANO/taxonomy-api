/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class NodeResource extends DomainEntity implements EntityWithPathConnection, SortableResourceConnection<Node> {

    @ManyToOne
    @JoinColumn(name = "node_id")
    private Node node;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "rank")
    private int rank;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE })
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    private NodeResource() {
        setPublicId(URI.create("urn:node-resource:" + UUID.randomUUID()));
    }

    public static NodeResource create(Node node, Resource resource) {
        return create(node, resource, false);
    }

    public static NodeResource create(Node node, Resource resource, boolean primary) {
        final var nodeResource = new NodeResource();

        nodeResource.node = node;
        nodeResource.resource = resource;
        nodeResource.setPrimary(primary);

        node.addNodeResource(nodeResource);
        resource.addNodeResource(nodeResource);

        return nodeResource;
    }

    public void disassociate() {
        final var node = this.node;
        final var resource = this.resource;

        final var wasPrimary = isPrimary();

        this.node = null;
        this.resource = null;

        if (node != null) {
            node.removeNodeResource(this);
        }

        if (resource != null) {
            resource.removeNodeResource(this);
        }
    }

    @Override
    public Optional<Node> getParent() {
        return getNode();
    }

    public Optional<Node> getNode() {
        return Optional.ofNullable(node);
    }

    public Optional<Boolean> isPrimary() {
        return Optional.of(primary);
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Optional<Resource> getResource() {
        return Optional.ofNullable(resource);
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public Optional<EntityWithPath> getConnectedParent() {
        return Optional.ofNullable(node);
    }

    @Override
    public Optional<EntityWithPath> getConnectedChild() {
        return Optional.ofNullable(resource);
    }

    @PreRemove
    public void preRemove() {
        disassociate();
    }

    @Override
    public Optional<Relevance> getRelevance() {
        return Optional.ofNullable(relevance);
    }

    @Override
    public void setRelevance(Relevance relevance) {
        this.relevance = relevance;
    }
}
