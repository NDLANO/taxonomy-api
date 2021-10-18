package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface NodeRepository extends TaxonomyRepository<Node> {
    @Query("SELECT DISTINCT n" + "   FROM Node n" + "   LEFT JOIN FETCH n.cachedPaths"
            + "   LEFT JOIN FETCH n.translations" + "   WHERE n.context = :context")
    List<Node> findAllByContextIncludingCachedUrlsAndTranslations(boolean context);

    @Query("SELECT DISTINCT n" + "   FROM Node n" + "   LEFT JOIN FETCH n.cachedPaths"
            + "   LEFT JOIN FETCH n.translations")
    List<Node> findAllIncludingCachedUrlsAndTranslations();

    @Query("SELECT DISTINCT n" + "   FROM Node n" + "   LEFT JOIN FETCH n.cachedPaths"
            + "   LEFT JOIN FETCH n.translations"
            + "   WHERE n.id IN (SELECT DISTINCT nc.parent.id from NodeConnection nc)"
            + "   AND n.id NOT IN (SELECT DISTINCT nc.child.id from NodeConnection nc)")
    List<Node> findAllRootsIncludingCachedUrlsAndTranslations();

    @Query("SELECT DISTINCT n" + "   FROM Node n" + "   LEFT JOIN FETCH n.cachedPaths"
            + "   LEFT JOIN FETCH n.translations" + "   WHERE n.nodeType = :nodeType")
    List<Node> findAllByNodeTypeIncludingCachedUrlsAndTranslations(NodeType nodeType);

    @Query("SELECT DISTINCT n" + "   FROM Node n" + "   LEFT JOIN FETCH n.cachedPaths"
            + "   LEFT JOIN FETCH n.translations" + "   WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query("SELECT DISTINCT n" + "   FROM Node n" + "   LEFT JOIN FETCH n.cachedPaths"
            + "   LEFT JOIN FETCH n.translations" + "   WHERE n.contentUri = :contentUri")
    List<Node> findAllByContentUriIncludingCachedUrlsAndTranslations(URI contentUri);

    @Query("SELECT DISTINCT n" + "   FROM Node n" + "   LEFT JOIN FETCH n.cachedPaths"
            + "   LEFT JOIN FETCH n.translations" + "   WHERE n.contentUri = :contentUri"
            + "   AND n.nodeType = :nodeType")
    List<Node> findAllByContentUriAndNodeTypeIncludingCachedUrlsAndTranslations(URI contentUri, NodeType nodeType);

    @Query("SELECT DISTINCT n" + "   FROM Node n" + "   WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingFilters(URI publicId);

    @Query("SELECT DISTINCT n" + "   FROM Node n" + "   LEFT JOIN FETCH n.cachedPaths"
            + "   WHERE n.publicId = :publicId")
    Optional<Node> findFirstByPublicIdIncludingCachedUrls(URI publicId);

    Optional<Node> findFirstByPublicId(URI publicId);
}
