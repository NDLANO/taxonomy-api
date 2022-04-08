/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.MappedSuperclass;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@MappedSuperclass
public abstract class EntityWithPath extends DomainObject {
    public abstract Set<CachedPath> getCachedPaths();

    public void addCachedPath(CachedPath cachedPath) {
        this.getCachedPaths().add(cachedPath);

        if (cachedPath.getOwningEntity().orElse(null) != this) {
            cachedPath.setOwningEntity(this);
        }
    }

    @Deprecated
    public void removeCachedPath(CachedPath cachedPath) {
        this.getCachedPaths().remove(cachedPath);

        if (cachedPath.getOwningEntity().orElse(null) == this) {
            cachedPath.setOwningEntity(null);
        }
    }

    public Optional<String> getPrimaryPath() {
        return getCachedPaths().stream().filter(CachedPath::isActive).filter(CachedPath::isPrimary)
                .map(CachedPath::getPath).findFirst();
    }

    public abstract Set<EntityWithPathConnection> getParentConnections();

    public abstract Set<EntityWithPathConnection> getChildConnections();

    public Optional<EntityWithPathConnection> getParentConnection() {
        return this.getParentConnections().stream().findFirst();
    }

    public Optional<String> getPathByContext(DomainEntity context) {
        final var contextPublicId = context.getPublicId();

        return getCachedPaths().stream().sorted((cachedUrl1, cachedUrl2) -> {
            final var path1 = cachedUrl1.getPath();
            final var path2 = cachedUrl2.getPath();

            final var path1MatchesContext = path1.startsWith("/" + contextPublicId.getSchemeSpecificPart());
            final var path2MatchesContext = path2.startsWith("/" + contextPublicId.getSchemeSpecificPart());

            final var path1IsPrimary = cachedUrl1.isPrimary();
            final var path2IsPrimary = cachedUrl2.isPrimary();

            if (path1IsPrimary && path2IsPrimary && path1MatchesContext && path2MatchesContext) {
                return 0;
            }

            if (path1MatchesContext && path2MatchesContext && path1IsPrimary) {
                return -1;
            }

            if (path1MatchesContext && path2MatchesContext && path2IsPrimary) {
                return 1;
            }

            if (path1MatchesContext && !path2MatchesContext) {
                return -1;
            }

            if (path2MatchesContext && !path1MatchesContext) {
                return 1;
            }

            if (path1IsPrimary && !path2IsPrimary) {
                return -1;
            }

            if (path2IsPrimary && !path1IsPrimary) {
                return 1;
            }

            return 0;
        }).map(CachedPath::getPath).findFirst();
    }

    public TreeSet<String> getAllPaths() {
        return getCachedPaths().stream().filter(CachedPath::isActive).map(CachedPath::getPath)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    abstract public Metadata getMetadata();

    abstract public URI getContentUri();

    abstract public void setContentUri(URI contentUri);

    /**
     * Checks if this entitiy can have a context (first element in a path)
     *
     * @return true if context
     */
    public abstract boolean isContext();
}
