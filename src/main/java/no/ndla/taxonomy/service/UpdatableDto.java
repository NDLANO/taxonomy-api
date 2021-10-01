/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.Optional;

public interface UpdatableDto<T> {
    default Optional<URI> getId() {
        return Optional.empty();
    }

    void apply(T entity);
}
