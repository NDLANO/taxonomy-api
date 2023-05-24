/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NodeRepository extends TaxonomyRepository<Node> {
    @Query("SELECT DISTINCT n FROM Node n WHERE n.context = :isContext")
    List<Node> findAllByContextIncludingCachedUrlsAndTranslations(boolean isContext);

    Optional<Node> findFirstByPublicId(URI publicId);

    @Query(value = "SELECT n.id FROM Node n ORDER BY n.id", countQuery = "SELECT count(*) from Node")
    Page<Integer> findIdsPaginated(Pageable pageable);

    @Query("""
            SELECT DISTINCT n FROM Node n
            LEFT JOIN FETCH n.resourceResourceTypes rrt
            LEFT JOIN FETCH rrt.resourceType rt
            LEFT JOIN FETCH n.parentConnections pc
            LEFT JOIN FETCH pc.relevance
            WHERE n.id in :ids
            """)
    List<Node> findByIds(Collection<Integer> ids);

    @Query("""
            SELECT DISTINCT n FROM Node n
            LEFT JOIN FETCH n.resourceResourceTypes rrt
            LEFT JOIN FETCH rrt.resourceType
            LEFT JOIN FETCH n.parentConnections pc
            LEFT JOIN FETCH pc.relevance
            WHERE n.id in :ids
            AND (:isVisible IS NULL OR n.visible = :isVisible)
            AND (:metadataFilterKey IS NULL OR jsonb_extract_path_text(n.customfields, :metadataFilterKey) IS NOT NULL)
            AND (:metadataFilterValue IS NULL OR cast(jsonb_path_query_array(n.customfields, '$.*') as text) like :metadataFilterValue)
            AND (:contentUri IS NULL OR n.contentUri = :contentUri)
            AND (:contextId IS NULL OR jsonb_contains(n.contexts, jsonb_build_array(jsonb_build_object('contextId',:contextId))) = true)
            AND (:isRoot IS NULL OR n.root = true)
            """)
    List<Node> findByIdsFiltered(Collection<Integer> ids, Optional<Boolean> isVisible,
            Optional<String> metadataFilterKey, Optional<String> metadataFilterValue, Optional<URI> contentUri,
            Optional<String> contextId, Optional<Boolean> isRoot);

    @Query("""
            SELECT n.id FROM Node n
            WHERE ((:nodeTypes) IS NULL OR n.nodeType in (:nodeTypes))
            AND (:isVisible IS NULL OR n.visible = :isVisible)
            AND (:metadataFilterKey IS NULL OR jsonb_extract_path_text(n.customfields, :metadataFilterKey) IS NOT NULL)
            AND (:metadataFilterValue IS NULL OR cast(jsonb_path_query_array(n.customfields, '$.*') as text) like :metadataFilterValue)
            AND (:contentUri IS NULL OR n.contentUri = :contentUri)
            AND (:contextId IS NULL OR jsonb_contains(n.contexts, jsonb_build_array(jsonb_build_object('contextId',:contextId))) = true)
            AND (:isRoot IS NULL OR n.root = true)
            """)
    List<Integer> findIdsFiltered(Optional<List<NodeType>> nodeTypes, Optional<Boolean> isVisible,
            Optional<String> metadataFilterKey, Optional<String> metadataFilterValue, Optional<URI> contentUri,
            Optional<String> contextId, Optional<Boolean> isRoot);

    @Query(value = "SELECT n.id FROM Node n where n.nodeType = :nodeType ORDER BY n.id", countQuery = "SELECT count(*) from Node n where n.nodeType = :nodeType")
    Page<Integer> findIdsByTypePaginated(Pageable pageable, NodeType nodeType);

    @Query("""
            SELECT n.id
            FROM Node n
            WHERE ((:nodeTypes) IS NULL OR n.nodeType in (:nodeTypes))
            """)
    List<Integer> findIdsByType(Optional<List<NodeType>> nodeTypes);

    @Query("""
            SELECT DISTINCT n FROM Node n
            LEFT JOIN FETCH n.resourceResourceTypes rrt
            LEFT JOIN FETCH rrt.resourceType
            LEFT JOIN FETCH n.parentConnections pc
            LEFT JOIN FETCH pc.relevance
            WHERE ((:nodeTypes) IS NULL OR n.nodeType in (:nodeTypes))
            AND (:isVisible IS NULL OR n.visible = :isVisible)
            AND (:metadataFilterKey IS NULL OR jsonb_extract_path_text(n.customfields, :metadataFilterKey) IS NOT NULL)
            AND (:metadataFilterValue IS NULL OR cast(jsonb_path_query_array(n.customfields, '$.*') as text) like :metadataFilterValue)
            AND (:contentUri IS NULL OR n.contentUri = :contentUri)
            AND (:contextId IS NULL OR jsonb_contains(n.contexts, jsonb_build_array(jsonb_build_object('contextId',:contextId))) = true)
            AND (:isRoot IS NULL OR n.root = true)
            """)
    List<Node> findByNodeType(Optional<List<NodeType>> nodeTypes, Optional<Boolean> isVisible,
            Optional<String> metadataFilterKey, Optional<String> metadataFilterValue, Optional<URI> contentUri,
            Optional<String> contextId, Optional<Boolean> isRoot);
}
