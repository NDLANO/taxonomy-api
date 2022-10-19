/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Node;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface NodeRepository extends TaxonomyRepository<Node> {
    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN n.cachedPaths JOIN FETCH n.metadata m"
            + " LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf LEFT JOIN cvf.customField"
            + " LEFT JOIN n.translations WHERE n.context = :context")
    List<Node> findAllByContextIncludingCachedUrlsAndTranslations(boolean context);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN n.cachedPaths JOIN FETCH n.metadata m"
            + " LEFT JOIN m.grepCodes LEFT JOIN FETCH m.customFieldValues cvf LEFT JOIN cvf.customField"
            + " LEFT JOIN n.translations WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN n.cachedPaths WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingCachedUrls(URI publicId);

    Optional<Node> findFirstByPublicId(URI publicId);

    @EntityGraph(value = Node.GRAPH, type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN n.cachedPaths WHERE n.publicId = :publicId")
    Optional<Node> fetchNodeGraphByPublicId(URI publicId);
}
