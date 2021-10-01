/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.net.URI;
import java.util.Optional;

public interface EntityWithPathConnection {
    URI getPublicId();

    int getRank();

    void setRank(int rank);

    Optional<Boolean> isPrimary();

    void setPrimary(boolean isPrimary);

    Optional<EntityWithPath> getConnectedParent();

    Optional<EntityWithPath> getConnectedChild();

    public Optional<Relevance> getRelevance();

    public void setRelevance(Relevance relevance);
}
