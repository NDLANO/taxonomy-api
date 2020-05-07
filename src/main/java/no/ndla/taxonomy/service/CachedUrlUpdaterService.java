package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithPath;

public interface CachedUrlUpdaterService {
    void updateCachedUrls(EntityWithPath entity);

    void clearCachedUrls(EntityWithPath entity);
}
