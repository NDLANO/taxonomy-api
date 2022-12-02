/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends TaxonomyRepository<Resource> {
    @Query("SELECT DISTINCT r FROM Resource r " + RESOURCE_METADATA + " LEFT JOIN FETCH r.cachedPaths"
            + " LEFT JOIN FETCH r.resourceTranslations WHERE r.publicId = :publicId")
    Optional<Resource> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    Optional<Resource> findFirstByPublicId(URI publicId);

    @Query("SELECT DISTINCT r FROM Resource r " + RESOURCE_METADATA + " LEFT JOIN FETCH r.cachedPaths"
            + " LEFT JOIN FETCH r.resourceTranslations WHERE r.contentUri = :contentUri")
    List<Resource> findByContentUriIncludingCachedUrlsAndTranslations(URI contentUri);

    @Query("SELECT DISTINCT r FROM Resource r " + RESOURCE_METADATA
            + " LEFT JOIN FETCH r.cachedPaths WHERE r.publicId = :publicId")
    Optional<Resource> findFirstByPublicIdIncludingCachedUrls(URI publicId);

    @Query("SELECT distinct r FROM Resource r " + RESOURCE_METADATA + " LEFT JOIN FETCH r.cachedPaths"
            + " LEFT JOIN FETCH r.resourceResourceTypes rrt LEFT JOIN FETCH rrt.resourceType rt"
            + " LEFT JOIN FETCH rt.resourceTypeTranslations LEFT JOIN FETCH r.resourceTranslations"
            + " WHERE r.id IN (:idSet)")
    List<Resource> findByIdIncludingCachedUrlsAndResourceTypesAndFiltersAndTranslations(Collection<Integer> idSet);

    @Query("SELECT r.id FROM Resource r")
    List<Integer> getAllResourceIds();

    @Query("SELECT r.id FROM Resource r LEFT JOIN r.metadata m WHERE m.visible = :visible")
    List<Integer> getAllResourceIdsWithMetadataVisible(Boolean visible);

    @Query("SELECT r.id FROM Resource r LEFT JOIN r.metadata m LEFT JOIN m.customFieldValues cfv"
            + " LEFT JOIN cfv.customField cf WHERE cf.key = :key")
    List<Integer> getAllResourceIdsWithMetadataKey(String key);

    @Query("SELECT r.id FROM Resource r LEFT JOIN r.metadata m LEFT JOIN m.customFieldValues cfv"
            + " LEFT JOIN cfv.customField cf WHERE cfv.value = :value")
    List<Integer> getAllResourceIdsWithMetadataValue(String value);

    @EntityGraph(value = Resource.GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT DISTINCT r FROM Resource r LEFT JOIN FETCH r.cachedPaths WHERE r.publicId = :publicId")
    Optional<Resource> fetchResourceGraphByPublicId(URI publicId);

    @Query(value = "SELECT r.id FROM Resource r ORDER BY r.id", countQuery = "SELECT count(*) from Resource")
    Page<Integer> findIdsPaginated(Pageable pageable);

    @Query("SELECT distinct r FROM Resource r " + RESOURCE_METADATA + " LEFT JOIN FETCH r.cachedPaths"
            + " LEFT JOIN FETCH r.resourceResourceTypes rrt LEFT JOIN FETCH rrt.resourceType rt"
            + " LEFT JOIN FETCH rt.resourceTypeTranslations LEFT JOIN FETCH r.resourceTranslations"
            + " WHERE r.id IN (:ids)")
    List<Resource> findByIds(Collection<Integer> ids);
}
