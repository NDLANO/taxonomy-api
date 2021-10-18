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
    @Query("SELECT DISTINCT r" + "   FROM Resource r" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   LEFT JOIN FETCH r.resourceTranslations" + "   WHERE r.publicId = :publicId")
    Optional<Resource> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query("SELECT distinct r" + "   FROM Resource r" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   LEFT JOIN FETCH r.resourceResourceTypes rrt" + "   LEFT JOIN FETCH rrt.resourceType rt"
            + "   LEFT JOIN FETCH rt.resourceTypeTranslations" + "   LEFT JOIN FETCH r.resourceTranslations"
            + "   WHERE r.contentUri = :contentUri")
    List<Resource> findAllByContentUriIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(URI contentUri);

    @Query("SELECT distinct r" + "   FROM Resource r" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   LEFT JOIN FETCH r.resourceResourceTypes rrt" + "   LEFT JOIN FETCH rrt.resourceType rt"
            + "   LEFT JOIN FETCH rt.resourceTypeTranslations" + "   LEFT JOIN FETCH r.resourceTranslations"
            + "   WHERE r.id IN (:idSet)")
    List<Resource> findByIdIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(Collection<Integer> idSet);

    @Query("SELECT distinct r" + "   FROM Resource r" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   LEFT JOIN FETCH r.resourceResourceTypes rrt" + "   LEFT JOIN FETCH rrt.resourceType rt"
            + "   LEFT JOIN FETCH rt.resourceTypeTranslations" + "   LEFT JOIN FETCH r.resourceTranslations"
            + "   WHERE r.publicId IN (:idSet)")
    List<Resource> findByPublicIdIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(Collection<URI> idSet);

    @Query("SELECT r.id FROM Resource r")
    List<Integer> getAllResourceIds();

    Optional<Resource> findFirstByPublicId(URI publicId);

    @Query("SELECT DISTINCT r" + "   FROM Resource r" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   WHERE r.publicId = :publicId")
    Optional<Resource> findFirstByPublicIdIncludingCachedUrls(URI publicId);

    boolean existsByPublicId(URI publicId);
}
