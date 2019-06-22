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
                    "   LEFT OUTER JOIN FETCH t.cachedUrls" +
                    "   LEFT OUTER JOIN FETCH t.translations" +
                    "   WHERE t.context = :context")
    List<Topic> findAllByContextIncludingCachedUrlsAndTranslations(boolean context);

    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT OUTER JOIN FETCH t.cachedUrls" +
                    "   LEFT OUTER JOIN FETCH t.translations")
    List<Topic> findAllIncludingCachedUrlsAndTranslations();

    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT OUTER JOIN FETCH t.cachedUrls" +
                    "   LEFT OUTER JOIN FETCH t.translations" +
                    "   WHERE t.publicId = :publicId")
    Optional<Topic> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query("SELECT DISTINCT t" +
            "   FROM Topic t" +
            "   LEFT OUTER JOIN FETCH t.cachedUrls" +
            "   LEFT OUTER JOIN FETCH t.translations" +
            "   WHERE t.contentUri = :contentUri")
    List<Topic> findAllByContentUriIncludingCachedUrlsAndTranslations(URI contentUri);

    @Query("SELECT DISTINCT t" +
            "   FROM Topic t" +
            "   LEFT OUTER JOIN FETCH t.cachedUrls" +
            "   WHERE t.publicId = :publicId")
    List<Topic> findAllByPublicIdIncludingCachedUrls(URI publicId);

    @Query("SELECT DISTINCT t" +
            "   FROM Topic t" +
            "   LEFT OUTER JOIN FETCH t.filters tf" +
            "   LEFT OUTER JOIN FETCH tf.filter")
    Optional<Topic> findFirstByPublicIdIncludingFilters(URI publicId);

    Optional<Topic> findFirstByPublicId(URI publicId);
}
