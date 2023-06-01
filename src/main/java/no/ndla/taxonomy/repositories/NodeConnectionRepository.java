/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import java.net.URI;
import java.util.*;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface NodeConnectionRepository extends TaxonomyRepository<NodeConnection> {
    @Query(
            """
            SELECT DISTINCT nc
            FROM NodeConnection nc
            JOIN FETCH nc.parent p
            JOIN FETCH nc.child c
            LEFT JOIN FETCH nc.relevance rel
            WHERE nc.parent.publicId IN :nodeId
            AND ((:nodeTypes) IS NULL OR c.nodeType in :nodeTypes)
            """)
    List<NodeConnection> findAllByNodeIdInIncludingTopicAndSubtopic(Set<URI> nodeId, List<NodeType> nodeTypes);

    @Query(
            """
            SELECT DISTINCT nc FROM NodeConnection nc
            LEFT JOIN FETCH nc.child child
            LEFT JOIN FETCH nc.parent n
            LEFT JOIN FETCH child.resourceResourceTypes rrt
            LEFT JOIN FETCH nc.relevance rel
            LEFT JOIN FETCH rrt.resourceType rt
            WHERE n.publicId IN :nodeIds
            AND (:resourceTypePublicIds IS NULL OR rt.publicId IN :resourceTypePublicIds)
            AND (:relevancePublicId IS NULL OR rel.publicId = :relevancePublicId)
            AND child.nodeType = 'RESOURCE'
            """)
    List<NodeConnection> getResourceBy(Set<URI> nodeIds, Set<URI> resourceTypePublicIds, URI relevancePublicId);

    @Query(
            """
            SELECT DISTINCT nc FROM NodeConnection nc
            JOIN FETCH nc.child r
            LEFT JOIN FETCH nc.parent n
            LEFT JOIN FETCH r.resourceResourceTypes rrt
            LEFT JOIN FETCH nc.relevance rel
            LEFT JOIN FETCH rrt.resourceType rt
            WHERE n.publicId IN :nodeIds
            AND r.nodeType = 'RESOURCE'
            """)
    List<NodeConnection> getByResourceIds(Collection<URI> nodeIds);

    @Query("SELECT nc FROM NodeConnection nc JOIN FETCH nc.parent JOIN FETCH nc.child")
    List<NodeConnection> findAllIncludingParentAndChild();

    @Query(
            "SELECT nc FROM NodeConnection nc JOIN FETCH nc.parent JOIN FETCH nc.child c WHERE c.nodeType = :childNodeType")
    List<NodeConnection> findAllByChildNodeType(NodeType childNodeType);

    @Query(
            value = "SELECT nc.id FROM NodeConnection nc ORDER BY nc.id",
            countQuery = "SELECT count(*) from NodeConnection")
    Page<Integer> findIdsPaginated(Pageable pageable);

    @Query(
            value =
                    """
            SELECT nc
            FROM NodeConnection nc
            JOIN nc.child c
            WHERE c.nodeType = :nodeType
            ORDER BY nc.id
            """,
            countQuery =
                    """
            SELECT count(nc)
            FROM NodeConnection nc
            JOIN nc.child c
            WHERE c.nodeType = :nodeType
            """)
    Page<NodeConnection> findIdsPaginatedByChildNodeType(Pageable pageable, NodeType nodeType);

    @Query("SELECT nc.id FROM NodeConnection nc")
    List<Integer> findAllIds();

    @Query("SELECT DISTINCT nc FROM NodeConnection nc JOIN FETCH nc.parent n JOIN FETCH nc.child c WHERE nc.id in :ids")
    List<NodeConnection> findByIds(Collection<Integer> ids);

    @Query(
            "SELECT DISTINCT nc FROM NodeConnection nc JOIN FETCH nc.child c JOIN FETCH nc.parent n WHERE n.publicId = :publicId")
    List<NodeConnection> findAllByParentPublicIdIncludingChildAndChildTranslations(URI publicId);

    @Query(
            """
            SELECT DISTINCT nc
            FROM NodeConnection nc
            JOIN FETCH nc.parent n
            JOIN FETCH nc.child c
            WHERE nc.child.publicId IN :nodeId""")
    List<NodeConnection> doFindAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(Collection<URI> nodeId);

    default List<NodeConnection> findAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(Collection<URI> nodeId) {
        if (nodeId.size() == 0) {
            return List.of();
        }

        return doFindAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(nodeId);
    }

    Optional<NodeConnection> findFirstByPublicId(URI publicId);

    @Query(
            """
            SELECT DISTINCT nc
            FROM NodeConnection nc
            JOIN FETCH nc.parent n
            JOIN FETCH nc.child c
            WHERE nc.parent.id = :parentId
            AND nc.child.id = :childId
            """)
    NodeConnection findByParentIdAndChildId(Integer parentId, Integer childId);
}
