package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Status;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface StatusRepository extends TaxonomyRepository<Status> {
    @Query(
            "SELECT DISTINCT s" +
                    "   FROM Status s" +
                    "   LEFT JOIN FETCH s.statusTranslations")
    List<Status> findAllIncludingTranslations();

    @Query(
            "SELECT DISTINCT s" +
                    "   FROM Status s" +
                    "   LEFT JOIN FETCH s.statusTranslations" +
                    "   WHERE s.publicId = :publicId")
    Optional<Status> findFirstByPublicIdIncludingTranslations(URI publicId);
}
