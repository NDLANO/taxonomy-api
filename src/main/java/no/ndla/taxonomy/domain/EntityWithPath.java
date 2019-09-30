package no.ndla.taxonomy.domain;

import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@MappedSuperclass
public abstract class EntityWithPath extends DomainObject {
    @OneToMany
    @JoinColumn(name = "publicId", referencedColumnName = "publicId", updatable = false, insertable = false)
    private final Set<CachedUrl> cachedUrls = new HashSet<>();

    public Set<CachedUrl> getCachedUrls() {
        return this.cachedUrls;
    }

    public Optional<String> getPrimaryPath() {
        return getCachedUrls()
                .stream()
                .map(CachedUrl::getPath)
                .findFirst();
    }

    public abstract Optional<EntityWithPathConnection> getParentConnection();

    public abstract Set<EntityWithPathConnection> getChildConnections();

    public Optional<String> getPathByContext(DomainEntity context) {
        final var contextPublicId = context.getPublicId();

        return getCachedUrls()
                .stream()
                .sorted((cachedUrl1, cachedUrl2) -> {
                    final var path1 = cachedUrl1.getPath();
                    final var path2 = cachedUrl2.getPath();

                    final var path1MatchesContext = path1.startsWith("/" + contextPublicId.getSchemeSpecificPart());
                    final var path2MatchesContext = path2.startsWith("/" + contextPublicId.getSchemeSpecificPart());

                    if (path1MatchesContext && path2MatchesContext) {
                        return 0;
                    }

                    if (path1MatchesContext) {
                        return -1;
                    }

                    if (path2MatchesContext) {
                        return 1;
                    }

                    return 0;
                })
                .map(CachedUrl::getPath)
                .findFirst();

    }

    public Set<String> getAllPaths() {
        return getCachedUrls().stream().map(CachedUrl::getPath).collect(Collectors.toSet());
    }

    abstract public URI getContentUri();
}