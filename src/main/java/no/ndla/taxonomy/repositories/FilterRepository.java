package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Filter;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
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
}
