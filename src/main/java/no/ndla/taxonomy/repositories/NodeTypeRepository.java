package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.NodeType;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

public interface NodeTypeRepository extends TaxonomyRepository<NodeType> {
    @Query("SELECT nt" +
            "   FROM NodeType nt" +
            "   LEFT JOIN FETCH nt.translations")
    Set<NodeType> findAllIncludingTranslations();

    @Query("SELECT nt" +
            "   FROM NodeType nt" +
            "   LEFT JOIN FETCH nt.translations" +
            "   WHERE nt.publicId = :publicId")
    Optional<NodeType> findFirstByPublicIdIncludingTranslations(URI publicId);
}
