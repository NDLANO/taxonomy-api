/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NodeConnectionRepository extends TaxonomyRepository<NodeConnection> {
    @Query("""
            SELECT DISTINCT nc
            FROM NodeConnection nc
            JOIN FETCH nc.parent p
            JOIN FETCH nc.child c
            LEFT JOIN FETCH nc.relevance rel
            WHERE nc.parent.id IN :nodeId
            AND ((:nodeTypes) IS NULL OR c.nodeType in :nodeTypes)
            """)
    List<NodeConnection> findAllByNodeIdInIncludingTopicAndSubtopic(Set<Integer> nodeId, List<NodeType> nodeTypes);

    @Query("""
            SELECT DISTINCT nc FROM NodeConnection nc
            LEFT JOIN FETCH nc.metadata ncm LEFT JOIN FETCH ncm.grepCodes LEFT JOIN FETCH ncm.customFieldValues nccfv LEFT JOIN FETCH nccfv.customField
            JOIN FETCH nc.child r
            LEFT JOIN FETCH r.metadata rm LEFT JOIN FETCH rm.grepCodes LEFT JOIN FETCH rm.customFieldValues rcfv LEFT JOIN FETCH rcfv.customField
            LEFT JOIN FETCH nc.parent n
            LEFT JOIN FETCH n.metadata nm LEFT JOIN FETCH nm.grepCodes LEFT JOIN FETCH nm.customFieldValues ncfv LEFT JOIN FETCH ncfv.customField
            LEFT JOIN nc.relevance rel
            LEFT JOIN r.resourceResourceTypes rrt
            LEFT JOIN rrt.resourceType rt
            LEFT JOIN r.cachedPaths
            LEFT JOIN FETCH r.resourceResourceTypes rrtFetch
            LEFT JOIN FETCH nc.relevance
            LEFT JOIN FETCH rrtFetch.resourceType rtFetch
            LEFT JOIN FETCH rtFetch.resourceTypeTranslations
            WHERE n.id IN :nodeIds
            AND (rt.publicId IN :resourceTypePublicIds)
            AND r.nodeType = 'RESOURCE'
            AND (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)
            """)
    List<NodeConnection> getResourceBy(Set<Integer> nodeIds, Set<URI> resourceTypePublicIds, URI relevancePublicId);

    @Query("SELECT DISTINCT nc FROM NodeConnection nc " + NODE_CONNECTION_METADATA + " LEFT JOIN FETCH nc.parent n "
            + NODE_METADATA + " LEFT JOIN FETCH nc.child r" + RESOURCE_METADATA
            + " LEFT JOIN r.cachedPaths LEFT JOIN FETCH r.resourceResourceTypes rrtFetch"
            + " LEFT JOIN FETCH rrtFetch.resourceType rtFetch LEFT JOIN FETCH rtFetch.resourceTypeTranslations"
            + " WHERE n.id IN :nodeIds AND r.nodeType = 'RESOURCE'")
    List<NodeConnection> getByResourceIds(Collection<Integer> nodeIds);

    @Query("SELECT nc FROM NodeConnection nc JOIN FETCH nc.parent JOIN FETCH nc.child JOIN FETCH nc.metadata m"
            + " LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf LEFT JOIN cvf.customField")
    List<NodeConnection> findAllIncludingParentAndChild();

    @Query("SELECT nc FROM NodeConnection nc JOIN FETCH nc.parent JOIN FETCH nc.child c JOIN FETCH nc.metadata m"
            + " LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf LEFT JOIN cvf.customField WHERE c.nodeType = :childNodeType")
    List<NodeConnection> findAllByChildNodeType(NodeType childNodeType);

    @Query(value = "SELECT nc.id FROM NodeConnection nc ORDER BY nc.id", countQuery = "SELECT count(*) from NodeConnection")
    Page<Integer> findIdsPaginated(Pageable pageable);

    @Query(value = """
            SELECT nc
            FROM NodeConnection nc
            JOIN nc.child c
            WHERE c.nodeType = :nodeType
            ORDER BY nc.id
            """, countQuery = """
            SELECT count(nc)
            FROM NodeConnection nc
            JOIN nc.child c
            WHERE c.nodeType = :nodeType
            """)
    Page<NodeConnection> findIdsPaginatedByChildNodeType(Pageable pageable, NodeType nodeType);

    @Query("SELECT DISTINCT nc FROM NodeConnection nc " + NODE_CONNECTION_METADATA + " JOIN FETCH nc.parent n "
            + NODE_METADATA + " JOIN FETCH nc.child c " + CHILD_METADATA + " WHERE nc.id in :ids")
    List<NodeConnection> findByIds(Collection<Integer> ids);

    @Query("SELECT DISTINCT nc FROM NodeConnection nc " + NODE_CONNECTION_METADATA + " JOIN FETCH nc.child c "
            + CHILD_METADATA + " JOIN FETCH nc.parent n" + NODE_METADATA
            + " LEFT JOIN FETCH c.translations WHERE n.publicId = :publicId")
    List<NodeConnection> findAllByParentPublicIdIncludingChildAndChildTranslations(URI publicId);

    @Query("SELECT DISTINCT nc FROM NodeConnection nc " + NODE_CONNECTION_METADATA + " JOIN FETCH nc.parent n "
            + NODE_METADATA + " JOIN FETCH nc.child c" + CHILD_METADATA
            + " LEFT JOIN n.translations LEFT JOIN FETCH c.translations LEFT JOIN c.cachedPaths"
            + " WHERE nc.child.id IN :nodeId")
    List<NodeConnection> doFindAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(Collection<Integer> nodeId);

    default List<NodeConnection> findAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(
            Collection<Integer> nodeId) {
        if (nodeId.size() == 0) {
            return List.of();
        }

        return doFindAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(nodeId);
    }

    Optional<NodeConnection> findFirstByPublicId(URI publicId);
}
