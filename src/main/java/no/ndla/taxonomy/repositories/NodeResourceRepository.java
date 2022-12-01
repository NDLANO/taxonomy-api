/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.NodeResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NodeResourceRepository extends TaxonomyRepository<NodeResource> {
    @Query("SELECT nr FROM NodeResource nr " + NODE_RESOURCE_METADATA + " JOIN FETCH nr.node n " + NODE_METADATA
            + " JOIN FETCH nr.resource r " + RESOURCE_METADATA)
    List<NodeResource> findAllIncludingNodeAndResource();

    @Query(value = "SELECT nr.id FROM NodeResource nr ORDER BY nr.id", countQuery = "SELECT count(*) FROM NodeResource")
    Page<Integer> findIdsPaginated(Pageable pageable);

    @Query("SELECT nr FROM NodeResource nr" + NODE_RESOURCE_METADATA + " JOIN FETCH nr.node n " + NODE_METADATA
            + " JOIN FETCH nr.resource r " + RESOURCE_METADATA + " WHERE nr.id in :ids")
    List<NodeResource> findByIds(Collection<Integer> ids);

    @Query("SELECT DISTINCT nr FROM NodeResource nr " + NODE_RESOURCE_METADATA + " LEFT JOIN FETCH nr.node n "
            + NODE_METADATA + " LEFT JOIN FETCH nr.resource r" + RESOURCE_METADATA
            + " LEFT JOIN r.cachedPaths LEFT JOIN FETCH r.resourceResourceTypes rrtFetch"
            + " LEFT JOIN FETCH rrtFetch.resourceType rtFetch LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
            + " WHERE n.id IN :nodeIds")
    List<NodeResource> findAllByNodeIdsIncludingRelationsForResourceDocuments(Collection<Integer> nodeIds);

    @Query("SELECT DISTINCT nr FROM NodeResource nr " + NODE_RESOURCE_METADATA + " JOIN FETCH nr.resource r "
            + RESOURCE_METADATA + " LEFT JOIN FETCH nr.node n " + NODE_METADATA
            + " LEFT JOIN nr.relevance rel LEFT JOIN r.resourceResourceTypes rrt"
            + " LEFT JOIN rrt.resourceType rt LEFT JOIN FETCH r.resourceTranslations LEFT JOIN r.cachedPaths"
            + " LEFT JOIN FETCH r.resourceResourceTypes rrtFetch LEFT JOIN FETCH nr.relevance"
            + " LEFT JOIN FETCH rrtFetch.resourceType rtFetch LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
            + " WHERE n.id IN :nodeIds AND (rt.publicId IN :resourceTypePublicIds) AND"
            + "   (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<NodeResource> doFindAllByNodeIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
            Collection<Integer> nodeIds, Collection<URI> resourceTypePublicIds, URI relevancePublicId);

    default List<NodeResource> findAllByNodeIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
            Collection<Integer> nodeIds, Collection<URI> resourceTypePublicIds, URI relevancePublicId) {
        if (nodeIds.size() == 0) {
            nodeIds = null;
        }

        if (resourceTypePublicIds.size() == 0) {
            resourceTypePublicIds = null;
        }

        return doFindAllByNodeIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                nodeIds, resourceTypePublicIds, relevancePublicId);
    }

    Optional<NodeResource> findFirstByPublicId(URI publicId);
}