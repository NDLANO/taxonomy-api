package no.ndla.taxonomy.domain;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface ResolvablePathEntity {
    Integer getId();

    String getType();

    Instant getUpdatedAt();

    Set<GeneratedPath> generatePaths(int iterations);

    default Set<GeneratedPath> generatePaths() {
        return generatePaths(0);
    }

    Set<ResolvablePathEntity> getChildren();

    URI getPublicId();

    String getName();

    URI getContentUri();

    Optional<ResolvedPath> getPrimaryResolvedPath();
}
