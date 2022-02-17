/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Resource;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends TaxonomyRepository<Resource> {
    @Query("SELECT DISTINCT r FROM Resource r LEFT JOIN FETCH r.cachedPaths"
            + " LEFT JOIN FETCH r.resourceTranslations WHERE r.publicId = :publicId")
    Optional<Resource> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    Optional<Resource> findFirstByPublicId(URI publicId);

    @Query("SELECT DISTINCT r FROM Resource r LEFT JOIN FETCH r.cachedPaths WHERE r.publicId = :publicId")
    Optional<Resource> findFirstByPublicIdIncludingCachedUrls(URI publicId);
}
