/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface NodeRepository extends TaxonomyRepository<Node> {
    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.cachedPaths"
            + " LEFT JOIN FETCH n.translations WHERE n.context = :context")
    List<Node> findAllByContextIncludingCachedUrlsAndTranslations(boolean context);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.cachedPaths" + " LEFT JOIN FETCH n.translations")
    List<Node> findAllIncludingCachedUrlsAndTranslations();

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.version v LEFT JOIN FETCH n.cachedPaths"
            + " LEFT JOIN FETCH n.translations WHERE n.root = true AND v.hash = :hash")
    List<Node> findAllRootsForVersionIncludingCachedUrlsAndTranslations(String hash);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.cachedPaths"
            + " LEFT JOIN FETCH n.translations WHERE n.nodeType = :nodeType")
    List<Node> findAllByNodeTypeIncludingCachedUrlsAndTranslations(NodeType nodeType);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.cachedPaths"
            + " LEFT JOIN FETCH n.translations WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.cachedPaths"
            + " LEFT JOIN FETCH n.translations WHERE n.contentUri = :contentUri")
    List<Node> findAllByContentUriIncludingCachedUrlsAndTranslations(URI contentUri);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.version v LEFT JOIN FETCH n.cachedPaths"
            + " LEFT JOIN FETCH n.translations WHERE n.publicId = :publicId and v.hash = :hash")
    Optional<Node> findFirstByPublicIdAndVersionIncludingCachedUrlsAndTranslations(URI publicId, String hash);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.cachedPaths"
            + " LEFT JOIN FETCH n.translations WHERE n.contentUri = :contentUri" + " AND n.nodeType = :nodeType")
    List<Node> findAllByContentUriAndNodeTypeIncludingCachedUrlsAndTranslations(URI contentUri, NodeType nodeType);

    @Query("SELECT DISTINCT n FROM Node n WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingFilters(URI publicId);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.cachedPaths WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingCachedUrls(URI publicId);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.version v"
            + " WHERE n.publicId = :publicId AND v.hash = :hash")
    Optional<Node> findFirstByPublicIdAndVersion(URI publicId, String hash);

    Optional<Node> findFirstByPublicId(URI publicId);
}
