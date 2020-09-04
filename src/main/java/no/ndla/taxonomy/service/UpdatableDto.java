package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.Optional;

public interface UpdatableDto<T> {
    default Optional<URI> getId() {
        return Optional.empty();
    }

    void apply(T entity);
}
