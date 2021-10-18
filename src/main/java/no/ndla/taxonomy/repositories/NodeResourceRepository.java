package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.NodeResource;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NodeResourceRepository extends TaxonomyRepository<NodeResource> {
    @Query("SELECT nr" + "   FROM NodeResource nr" + "   JOIN FETCH nr.node" + "   JOIN FETCH nr.resource")
    List<NodeResource> findAllIncludingNodeAndResource();

    @Query("SELECT DISTINCT nr" + "   FROM NodeResource nr" + "   LEFT JOIN FETCH nr.node n"
            + "   LEFT JOIN FETCH nr.resource r" + "   LEFT JOIN FETCH nr.relevance rel"
            + "   LEFT JOIN FETCH r.resourceTranslations" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch" + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch"
            + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations" + "   WHERE n.id IN :nodeIds")
    List<NodeResource> findAllByNodeIdsIncludingRelationsForResourceDocuments(Collection<Integer> nodeIds);

    @Query("SELECT DISTINCT nr" + "   FROM NodeResource nr" + "   LEFT JOIN FETCH nr.node n"
            + "   LEFT JOIN FETCH nr.resource r" + "   LEFT JOIN nr.relevance rel"
            + "   LEFT JOIN FETCH r.resourceTranslations" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch" + "   LEFT JOIN FETCH nr.relevance"
            + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch" + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
            + "   WHERE " + "       n.id IN :nodeIds AND"
            + "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<NodeResource> doFindAllByNodeIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
            Collection<Integer> nodeIds, URI relevancePublicId);

    default List<NodeResource> findAllByNodeIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
            Collection<Integer> nodeIds, URI relevancePublicId) {
        if (nodeIds.size() == 0) {
            return doFindAllByNodeIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(null,
                    relevancePublicId);
        }

        return doFindAllByNodeIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(nodeIds,
                relevancePublicId);
    }

    @Query("SELECT DISTINCT nr" + "   FROM NodeResource nr" + "   JOIN FETCH nr.resource r"
            + "   LEFT JOIN FETCH nr.node n" + "   LEFT JOIN nr.relevance rel"
            + "   LEFT JOIN FETCH r.resourceTranslations" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch" + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch"
            + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations" + "   WHERE n.id IN :nodeIds AND "
            + "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<NodeResource> doFindAllByNodeIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
            Collection<Integer> nodeIds, URI relevancePublicId);

    default List<NodeResource> findAllByNodeIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
            Collection<Integer> nodeIds, URI relevancePublicId) {
        if (nodeIds.size() == 0) {
            nodeIds = null;
        }

        return doFindAllByNodeIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                nodeIds, relevancePublicId);
    }

    @Query("SELECT DISTINCT nr" + "   FROM NodeResource nr" + "   INNER JOIN FETCH nr.resource r"
            + "   LEFT JOIN FETCH nr.node n" + "   LEFT JOIN nr.relevance rel"
            + "   LEFT JOIN r.resourceResourceTypes rrt " + "   LEFT JOIN rrt.resourceType rt"
            + "   LEFT JOIN FETCH r.resourceTranslations" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch" + "   LEFT JOIN FETCH nr.relevance"
            + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch" + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
            + "   WHERE n.id IN :nodeIds AND" + "       (nr.publicId IN :resourceTypePublicIds) AND"
            + "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
    List<NodeResource> doFindAllByNodeIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
            Collection<Integer> nodeIds, Set<URI> resourceTypePublicIds, URI relevancePublicId);

    default List<NodeResource> findAllByNodeIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
            Collection<Integer> nodeIds, Set<URI> resourceTypePublicIds, URI relevancePublicId) {
        if (nodeIds.size() == 0) {
            nodeIds = null;
        }

        if (resourceTypePublicIds.size() == 0) {
            resourceTypePublicIds = null;
        }

        return doFindAllByNodeIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(
                nodeIds, resourceTypePublicIds, relevancePublicId);
    }

    @Query("SELECT DISTINCT nr" + "   FROM NodeResource nr" + "   JOIN FETCH nr.resource r"
            + "   LEFT JOIN FETCH nr.node n" + "   LEFT JOIN nr.relevance rel"
            + "   LEFT JOIN r.resourceResourceTypes rrt " + "   LEFT JOIN rrt.resourceType rt"
            + "   LEFT JOIN FETCH r.resourceTranslations" + "   LEFT JOIN FETCH r.cachedPaths"
            + "   LEFT JOIN FETCH r.resourceResourceTypes rrtFetch" + "   LEFT JOIN FETCH nr.relevance"
            + "   LEFT JOIN FETCH rrtFetch.resourceType rtFetch" + "   LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
            + "   WHERE n.id IN :nodeIds AND" + "       (rt.publicId IN :resourceTypePublicIds) AND"
            + "       (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)")
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