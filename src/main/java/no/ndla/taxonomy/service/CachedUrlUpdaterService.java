/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;

public interface CachedUrlUpdaterService {
    void updateCachedUrls(Node entity);

    void clearCachedUrls(Node entity);
}
