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
    @Query("SELECT DISTINCT n FROM Node n " + NODE_METADATA + " LEFT JOIN FETCH n.cachedPaths"
            + " LEFT JOIN FETCH n.translations WHERE n.context = :context")
    List<Node> findAllByContextIncludingCachedUrlsAndTranslations(boolean context);

    @Query("SELECT DISTINCT n FROM Node n " + NODE_METADATA + " LEFT JOIN FETCH n.cachedPaths"
            + " LEFT JOIN FETCH n.translations WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query("SELECT DISTINCT n FROM Node n " + NODE_METADATA
            + " LEFT JOIN FETCH n.cachedPaths WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingCachedUrls(URI publicId);

    Optional<Node> findFirstByPublicId(URI publicId);

    @Query(value = "SELECT n.id FROM Node n ORDER BY n.id", countQuery = "SELECT count(*) from Node")
    Page<Integer> findIdsPaginated(Pageable pageable);

    @Query("""
            SELECT n FROM Node n
            LEFT JOIN FETCH n.resourceResourceTypes rrt
            LEFT JOIN FETCH rrt.resourceType rt
            LEFT JOIN FETCH rt.resourceTypeTranslations
            LEFT JOIN FETCH n.metadata nm
            LEFT JOIN FETCH nm.grepCodes
            LEFT JOIN FETCH nm.customFieldValues ncfv
            LEFT JOIN FETCH ncfv.customField cf
            LEFT JOIN FETCH n.cachedPaths
            LEFT JOIN FETCH n.translations
            LEFT JOIN FETCH n.parentConnections pc
            LEFT JOIN FETCH pc.relevance rel
            LEFT JOIN FETCH rel.translations
            WHERE n.id in :ids
            """)
    List<Node> findByIds(Collection<Integer> ids);

    @Query(value = "SELECT n.id FROM Node n where n.nodeType = :nodeType ORDER BY n.id", countQuery = "SELECT count(*) from Node n where n.nodeType = :nodeType")
    Page<Integer> findIdsByTypePaginated(Pageable pageable, NodeType nodeType);

    @Query("SELECT n.id FROM Node n where n.nodeType in :nodeTypes ORDER BY n.id")
    List<Integer> findIdsByType(List<NodeType> nodeTypes);

    @Query("""
            SELECT DISTINCT n FROM Node n
            LEFT JOIN FETCH n.resourceResourceTypes rrt
            LEFT JOIN FETCH rrt.resourceType
            LEFT JOIN FETCH n.metadata nm
            LEFT JOIN FETCH nm.grepCodes
            LEFT JOIN FETCH nm.customFieldValues ncfv
            LEFT JOIN FETCH ncfv.customField cf
            LEFT JOIN FETCH n.cachedPaths
            LEFT JOIN FETCH n.translations
            LEFT JOIN FETCH n.parentConnections pc
            LEFT JOIN FETCH pc.relevance
            WHERE ((:nodeTypes) IS NULL OR n.nodeType in (:nodeTypes))
            AND (:isVisible IS NULL OR nm.visible = :isVisible)
            AND (:metadataFilterKey IS NULL OR cf.key = :metadataFilterKey)
            AND (:metadataFilterValue IS NULL OR ncfv.value = :metadataFilterValue)
            AND (:contentUri IS NULL OR n.contentUri = :contentUri)
            AND (:isRoot IS NULL OR n.root = true)
            """)
    List<Node> findByNodeType(List<NodeType> nodeTypes, Optional<Boolean> isVisible, Optional<String> metadataFilterKey,
            Optional<String> metadataFilterValue, Optional<URI> contentUri, Optional<Boolean> isRoot);
}
