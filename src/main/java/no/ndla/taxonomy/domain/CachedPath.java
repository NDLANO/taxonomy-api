/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "cached_path")
public class CachedPath {
    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "is_active")
    private boolean active = true;

    @Column
    @Id
    private UUID id;

    @Column
    private URI publicId;

    @Column
    private String path;

    @ManyToOne
    private Node node;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public URI getPublicId() {
        return publicId;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void disable() {
        setActive(false);
    }

    public Optional<EntityWithPath> getOwningEntity() {
        return Optional.ofNullable(node);
    }

    public void setOwningEntity(EntityWithPath entity) {
        if (entity == null) {
            this.setNode(null);
            return;
        }

        if (entity instanceof Node) {
            this.setNode((Node) entity);
        } else {
            throw new IllegalArgumentException("Unknown entity of type " + entity.getClass().toString()
                    + " passed as owning entity of CachedPath");
        }

        setPublicId(entity.getPublicId());
    }

    public Optional<Node> getNode() {
        return Optional.ofNullable(this.node);
    }

    public void setNode(Node node) {
        final var oldNode = this.node;

        if (node == null && oldNode == null) {
            return;
        }

        this.node = node;

        if (oldNode != null && oldNode != node) {
            oldNode.removeCachedPath(this);
        }

        if (node != null && !node.getCachedPaths().contains(this)) {
            node.addCachedPath(this);
        }
    }

}
