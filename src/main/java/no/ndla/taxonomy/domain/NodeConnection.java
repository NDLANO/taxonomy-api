package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class NodeConnection extends DomainEntity implements EntityWithPathConnection {
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Node parent;

    @ManyToOne
    @JoinColumn(name = "child_id")
    private Node child;

    @Column(name = "rank")
    private int rank;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE })
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    private NodeConnection() {
        setPublicId(URI.create("urn:node-connection:" + UUID.randomUUID()));
    }

    public static NodeConnection create(Node parent, Node child) {
        if (child == null || parent == null) {
            throw new NullPointerException();
        }

        final var nodeConnection = new NodeConnection();
        nodeConnection.parent = parent;
        nodeConnection.child = child;

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

    public Optional<Node> getParent() {
        return Optional.ofNullable(parent);
    }

    public Optional<Node> getChild() {
        return Optional.ofNullable(child);
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
        return Optional.of(true);
    }

    @Override
    public void setPrimary(boolean isPrimary) {
        if (isPrimary) {
            return;
        }

        throw new UnsupportedOperationException("NodeConnection can not be non-primary");
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
