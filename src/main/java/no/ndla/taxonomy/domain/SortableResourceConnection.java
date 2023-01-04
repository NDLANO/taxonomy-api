/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.util.Optional;

public interface SortableResourceConnection<T extends DomainEntity> {
    Optional<Node> getResource();

    Optional<T> getParent();

    int getRank();
}
