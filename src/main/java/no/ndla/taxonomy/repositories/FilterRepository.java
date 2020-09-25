package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Filter;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FilterRepository extends TaxonomyRepository<Filter> {
    @Query("SELECT DISTINCT f FROM Filter f" +
            "   LEFT JOIN FETCH f.subject" +
            "   LEFT JOIN f.translations")
    List<Filter> findAllWithSubjectAndTranslations();

    @Query("SELECT DISTINCT f FROM Filter f" +
            "   LEFT JOIN FETCH f.subject" +
            "   LEFT JOIN f.translations" +
            "   WHERE f.publicId = :publicId")
    Optional<Filter> findFirstByPublicIdWithSubjectAndTranslations(URI publicId);

    @Query("SELECT f FROM Filter f WHERE f.subject.publicId = :subjectPublicId")
    Collection<Filter> findAllBySubjectPublicId(URI subjectPublicId);

    @Query("SELECT DISTINCT f FROM Filter f" +
            "   LEFT JOIN FETCH f.subject" +
            "   LEFT JOIN FETCH f.translations" +
            "   WHERE f.subject.publicId = :subjectPublicId")
    Collection<Filter> findAllBySubjectPublicIdIncludingSubjectAndTranslations(URI subjectPublicId);
}
