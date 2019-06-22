package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Relevance;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

public interface RelevanceRepository extends TaxonomyRepository<Relevance> {
    @Query("SELECT r" +
            "   FROM Relevance r" +
            "   LEFT OUTER JOIN FETCH r.translations")
    Set<Relevance> findAllIncludingTranslations();

    @Query("SELECT r" +
            "   FROM Relevance r" +
            "   LEFT OUTER JOIN FETCH r.translations" +
            "   WHERE r.publicId = :publicId")
    Optional<Relevance> findFirstByPublicIdIncludingTranslations(URI publicId);
}
