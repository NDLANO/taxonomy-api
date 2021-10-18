package no.ndla.taxonomy.domain;

import java.util.Optional;

public interface SortableResourceConnection<T extends DomainEntity> {
    Optional<Resource> getResource();

    Optional<T> getParent();

    int getRank();
}
