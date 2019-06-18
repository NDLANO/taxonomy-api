package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Topic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface TopicRepository extends TaxonomyRepository<Topic>, TaxonomyRepositoryCustom<Topic> {
    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT OUTER JOIN FETCH t.resolvedPaths" +
                    "   LEFT OUTER JOIN FETCH t.translations" +
                    "   WHERE t.context = :context")
    List<Topic> findAllByContextIncludingResolvedPathsAndTranslations(boolean context);

    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT OUTER JOIN FETCH t.resolvedPaths" +
                    "   LEFT OUTER JOIN FETCH t.translations")
    List<Topic> findAllIncludingResolvedPathsAndTranslations();

    @Query(
            "SELECT DISTINCT t" +
                    "   FROM Topic t" +
                    "   LEFT OUTER JOIN FETCH t.resolvedPaths" +
                    "   LEFT OUTER JOIN FETCH t.translations" +
                    "   WHERE t.publicId = :publicId")
    Optional<Topic> findFirstByPublicIdIncludingResolvedPathsAndTranslations(URI publicId);

    @Query("SELECT DISTINCT t" +
            "   FROM Topic t" +
            "   LEFT OUTER JOIN FETCH t.resolvedPaths" +
            "   LEFT OUTER JOIN FETCH t.translations" +
            "   WHERE t.contentUri = :contentUri")
    List<Topic> findAllByContentUriIncludingResolvedPathAndTranslations(URI contentUri);

    @Query("SELECT DISTINCT t" +
            "   FROM Topic t" +
            "   LEFT OUTER JOIN FETCH t.resolvedPaths" +
            "   WHERE t.publicId = :publicId")
    List<Topic> findAllByPublicIdIncludingResolvedPaths(URI publicId);
}
