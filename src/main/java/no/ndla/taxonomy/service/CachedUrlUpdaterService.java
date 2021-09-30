/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithPath;

public interface CachedUrlUpdaterService {
    void updateCachedUrls(EntityWithPath entity);

    void clearCachedUrls(EntityWithPath entity);
}
