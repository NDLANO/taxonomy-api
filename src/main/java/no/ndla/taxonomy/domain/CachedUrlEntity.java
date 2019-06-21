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
public abstract class CachedUrlEntity extends DomainObject {
    @OneToMany
    @JoinColumn(name = "publicId", referencedColumnName = "publicId", updatable = false, insertable = false)
    private Set<CachedUrl> cachedUrls = new HashSet<>();

    public Set<CachedUrl> getCachedUrls() {
        return this.cachedUrls;
    }

    public Optional<String> getPrimaryPath() {
        return getCachedUrls()
                .stream()
                .filter(CachedUrl::isPrimary)
                .map(CachedUrl::getPath)
                .findFirst();
    }

    public Set<String> getAllPaths() {
        return getCachedUrls().stream().map(CachedUrl::getPath).collect(Collectors.toSet());
    }

    abstract public URI getContentUri();
}
