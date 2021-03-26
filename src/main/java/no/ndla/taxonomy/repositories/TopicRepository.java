package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface TopicRepository extends TaxonomyRepository<Topic> {
    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT JOIN FETCH t.cachedPaths" +
                    "   LEFT JOIN FETCH t.translations" +
                    "   WHERE t.context = :context")
    List<Topic> findAllByContextIncludingCachedUrlsAndTranslations(boolean context);

    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT JOIN FETCH t.cachedPaths" +
                    "   LEFT JOIN FETCH t.translations")
    List<Topic> findAllIncludingCachedUrlsAndTranslations();

    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT JOIN FETCH t.cachedPaths" +
                    "   LEFT JOIN FETCH t.translations" +
                    "   LEFT JOIN FETCH t.nodeType" +
                    "   WHERE t.nodeType.publicId = :nodeType")
    List<Topic> findAllByNodeTypeIncludingCachedUrlsAndTranslations(URI nodeType);

    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT JOIN FETCH t.cachedPaths" +
                    "   LEFT JOIN FETCH t.translations" +
                    "   WHERE t.publicId = :publicId")
    Optional<Topic> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT JOIN FETCH t.cachedPaths" +
                    "   LEFT JOIN FETCH t.translations" +
                    "   LEFT JOIN FETCH t.nodeType" +
            "   WHERE t.publicId = :publicId AND t.nodeType.publicId = :nodeType")
    Optional<Topic> findFirstByPublicIdAndNodeTypeIncludingCachedUrlsAndTranslations(URI publicId, URI nodeType);

    @Query("SELECT DISTINCT t" +
            "   FROM Topic t" +
            "   LEFT JOIN FETCH t.cachedPaths" +
            "   LEFT JOIN FETCH t.translations" +
            "   WHERE t.contentUri = :contentUri")
    List<Topic> findAllByContentUriIncludingCachedUrlsAndTranslations(URI contentUri);

    @Query("SELECT DISTINCT t" +
            "   FROM Topic t" +
            "   LEFT JOIN FETCH t.cachedPaths" +
            "   LEFT JOIN FETCH t.translations" +
            "   LEFT JOIN FETCH t.nodeType" +
            "   WHERE t.contentUri = :contentUri AND t.nodeType.publicId = :nodeType")
    List<Topic> findAllByContentUriAndNodeTypeIncludingCachedUrlsAndTranslations(URI contentUri, URI nodeType);

    @Query("SELECT DISTINCT t" +
            "   FROM Topic t" +
            "   WHERE t.publicId = :publicId")
    Optional<Topic> findFirstByPublicIdIncludingFilters(URI publicId);

    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT JOIN FETCH t.cachedPaths" +
                    "   WHERE t.publicId = :publicId")
    Optional<Topic> findFirstByPublicIdIncludingCachedUrls(URI publicId);


    Optional<Topic> findFirstByPublicId(URI publicId);
}
