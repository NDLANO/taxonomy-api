/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.NodeResource;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NodeResourceRepository extends TaxonomyRepository<NodeResource> {
    @Query("SELECT nr FROM NodeResource nr JOIN FETCH nr.node JOIN FETCH nr.resource JOIN FETCH nr.metadata m"
            + " LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf LEFT JOIN cvf.customField")
    List<NodeResource> findAllIncludingNodeAndResource();

    @Query("SELECT DISTINCT nr FROM NodeResource nr LEFT JOIN FETCH nr.node n LEFT JOIN FETCH nr.resource r"
            + " JOIN FETCH nr.metadata m LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf"
            + " LEFT JOIN cvf.customField LEFT JOIN FETCH nr.relevance rel LEFT JOIN r.resourceTranslations"
            + " LEFT JOIN r.cachedPaths LEFT JOIN FETCH r.resourceResourceTypes rrtFetch"
            + " LEFT JOIN FETCH rrtFetch.resourceType rtFetch LEFT JOIN rtFetch.resourceTypeTranslations"
            + " WHERE n.id IN :nodeIds")
    List<NodeResource> findAllByNodeIdsIncludingRelationsForResourceDocuments(Collection<Integer> nodeIds);

    @Query("SELECT DISTINCT nr FROM NodeResource nr JOIN FETCH nr.resource r LEFT JOIN FETCH nr.node n"
            + " JOIN FETCH nr.metadata m LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf"
            + " LEFT JOIN cvf.customField LEFT JOIN nr.relevance rel LEFT JOIN r.resourceResourceTypes rrt"
            + " LEFT JOIN rrt.resourceType rt LEFT JOIN r.resourceTranslations LEFT JOIN r.cachedPaths"
            + " LEFT JOIN FETCH r.resourceResourceTypes rrtFetch LEFT JOIN FETCH nr.relevance"
            + " LEFT JOIN FETCH rrtFetch.resourceType rtFetch LEFT JOIN rtFetch.resourceTypeTranslations"
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