package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Subject;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends TaxonomyRepository<Subject>, TaxonomyRepositoryCustom<Subject> {
    @Query(
            "SELECT DISTINCT s" +
                    "   FROM Subject s" +
                    "   LEFT OUTER JOIN FETCH s.resolvedPaths" +
                    "   LEFT OUTER JOIN FETCH s.translations")
    List<Subject> findAllIncludingResolvedPathsAndTranslations();

    @Query(
            "SELECT DISTINCT s" +
                    "   FROM Subject s" +
                    "   LEFT OUTER JOIN FETCH s.resolvedPaths" +
                    "   LEFT OUTER JOIN FETCH s.translations" +
                    "   WHERE s.publicId = :publicId")
    Optional<Subject> findFirstByPublicIdIncludingResolvedPathsAndTranslations(URI publicId);

    @Query("SELECT DISTINCT s" +
            "   FROM Subject s" +
            "   LEFT OUTER JOIN FETCH s.resolvedPaths" +
            "   WHERE s.publicId = :publicId")
    List<Subject> findAllByPublicIdIncludingResolvedPaths(URI publicId);
}
