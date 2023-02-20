/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.io.Serializable;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CachedPath implements Serializable {
    private boolean primary;
    private String publicId;
    private String path;

    private static CachedPath fromNonnullPath(String path, boolean isPrimary) {
        var cp = new CachedPath();
        var splitted = path.split("/");
        var publicId = "urn:" + splitted[splitted.length - 1];
        cp.setPublicId(URI.create(publicId));
        cp.setPath(path);
        cp.setPrimary(isPrimary);
        return cp;
    }

    public static Set<CachedPath> fromPaths(String[] primary, String[] secondary) {
        var paths = new HashSet<CachedPath>();

        if (primary != null) {
            for (var p : primary) {
                paths.add(CachedPath.fromNonnullPath(p, true));
            }
        }

        if (secondary != null) {
            for (var p : secondary) {
                paths.add(CachedPath.fromNonnullPath(p, false));
            }
        }

        return paths;
    }

    private Node node = null;

    public URI getPublicId() {
        if (publicId == null)
            return null;
        return URI.create(publicId);
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId.toString();
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
