package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Subject;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends TaxonomyRepository<Subject> {
    @Query(
            "SELECT DISTINCT s" +
                    "   FROM Subject s" +
                    "   LEFT JOIN FETCH s.cachedUrls" +
                    "   LEFT JOIN FETCH s.translations")
    List<Subject> findAllIncludingCachedUrlsAndTranslations();

    @Query(
            "SELECT DISTINCT s" +
                    "   FROM Subject s" +
                    "   LEFT JOIN FETCH s.cachedUrls" +
                    "   LEFT JOIN FETCH s.translations" +
                    "   WHERE s.publicId = :publicId")
    Optional<Subject> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query("SELECT DISTINCT s" +
            "   FROM Subject s" +
            "   LEFT JOIN FETCH s.cachedUrls" +
            "   WHERE s.publicId = :publicId")
    List<Subject> findAllByPublicIdIncludingCachedUrls(URI publicId);

    @Query("SELECT DISTINCT s " +
            "   FROM Subject s" +
            "   LEFT JOIN FETCH s.filters" +
            "   WHERE s.publicId = :publicId")
    Optional<Subject> findFirstByPublicIdIncludingFilters(URI publicId);

    Optional<Subject> findFirstByPublicId(URI publicId);
}
