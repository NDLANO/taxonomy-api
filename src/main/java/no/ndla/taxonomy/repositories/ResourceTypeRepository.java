package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.ResourceType;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface ResourceTypeRepository extends TaxonomyRepository<ResourceType> {
    @Query(
            "SELECT DISTINCT rt" +
                    "   FROM ResourceType rt" +
                    "   LEFT JOIN FETCH rt.subtypes" +
                    "   LEFT JOIN FETCH rt.resourceTypeTranslations" +
                    "   WHERE (:parent IS NULL AND rt.parent IS NULL) OR rt.parent = :parent")
    List<ResourceType> findAllByParentIncludingTranslationsAndFirstLevelSubtypes(ResourceType parent);

    @Query(
            "SELECT DISTINCT rt" +
                    "   FROM ResourceType rt" +
                    "   LEFT JOIN FETCH rt.resourceTypeTranslations" +
                    "   WHERE rt.publicId = :publicId")
    Optional<ResourceType> findFirstByPublicIdIncludingTranslations(URI publicId);

    @Query(
            "SELECT DISTINCT rt" +
                    "   FROM ResourceType rt" +
                    "   JOIN rt.parent prt" +
                    "   LEFT JOIN FETCH rt.resourceTypeTranslations" +
                    "   LEFT JOIN FETCH rt.subtypes" +
                    "   WHERE prt.publicId = :publicId")
    List<ResourceType> findAllByParentPublicIdIncludingTranslationsAndFirstLevelSubtypes(URI publicId);
}
