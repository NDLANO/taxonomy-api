package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Resource;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends TaxonomyRepository<Resource>, TaxonomyRepositoryCustom<Resource> {
    @Query(
            "SELECT DISTINCT r" +
                    "   FROM Resource r" +
                    "   LEFT OUTER JOIN FETCH r.resolvedPaths" +
                    "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
                    "   WHERE r.publicId = :publicId")
    Optional<Resource> findFirstByPublicIdIncludingResolvedPathsAndTranslations(URI publicId);

    @Query(
            "SELECT DISTINCT r" +
                    "   FROM Resource r" +
                    "   LEFT OUTER JOIN FETCH r.resolvedPaths" +
                    "   LEFT OUTER JOIN FETCH r.resourceTranslations")
    List<Resource> findAllIncludingResolvedPathsAndTranslations();

    @Query(
            "SELECT distinct r" +
                    "   FROM Resource r" +
                    "   LEFT OUTER JOIN FETCH r.resolvedPaths" +
                    "   LEFT OUTER JOIN FETCH r.resourceResourceTypes rrt" +
                    "   LEFT OUTER JOIN FETCH rrt.resourceType rt" +
                    "   LEFT OUTER JOIN FETCH rt.resourceTypeTranslations" +
                    "   LEFT OUTER JOIN FETCH r.resourceTranslations" +
                    "   WHERE r.contentUri = :contentUri"
    )
    List<Resource> findAllByContentUriIncludingResolvedPathsAndResourceTypesAndTranslations(URI contentUri);

    @Query("SELECT DISTINCT r" +
            "   FROM Resource r" +
            "   LEFT OUTER JOIN FETCH r.resolvedPaths" +
            "   WHERE r.publicId = :publicId")
    List<Resource> findAllByPublicIdIncludingResolvedPaths(URI publicId);
}
