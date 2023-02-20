/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.Node;

public interface CachedUrlUpdaterService<T extends EntityWithPath> {
    void updateCachedUrls(T entity);

    void clearCachedUrls(T entity);
}
