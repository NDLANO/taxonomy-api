package no.ndla.taxonomy.repositories;


import no.ndla.taxonomy.domain.Resource;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends TaxonomyRepository<Resource> {
    @Query(
            "SELECT DISTINCT r" +
                    "   FROM Resource r" +
                    "   LEFT JOIN FETCH r.cachedUrls" +
                    "   LEFT JOIN FETCH r.resourceTranslations" +
                    "   WHERE r.publicId = :publicId")
    Optional<Resource> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query(
            "SELECT DISTINCT r" +
                    "   FROM Resource r" +
                    "   LEFT JOIN FETCH r.cachedUrls" +
                    "   LEFT JOIN FETCH r.resourceTranslations")
    List<Resource> findAllIncludingCachedUrlsAndTranslations();

    @Query(
            "SELECT distinct r" +
                    "   FROM Resource r" +
                    "   LEFT JOIN FETCH r.cachedUrls" +
                    "   LEFT JOIN FETCH r.resourceResourceTypes rrt" +
                    "   LEFT JOIN FETCH rrt.resourceType rt" +
                    "   LEFT JOIN FETCH rt.resourceTypeTranslations" +
                    "   LEFT JOIN FETCH r.resourceTranslations" +
                    "   WHERE r.contentUri = :contentUri"
    )
    List<Resource> findAllByContentUriIncludingCachedUrlsAndResourceTypesAndTranslations(URI contentUri);

    @Query("SELECT DISTINCT r" +
            "   FROM Resource r" +
            "   LEFT JOIN FETCH r.cachedUrls" +
            "   WHERE r.publicId = :publicId")
    List<Resource> findAllByPublicIdIncludingCachedUrls(URI publicId);

    Optional<Resource> findFirstByPublicId(URI publicId);

    @Query(
            "SELECT DISTINCT r" +
                    "   FROM Resource r" +
                    "   LEFT JOIN FETCH r.cachedUrls" +
                    "   WHERE r.publicId = :publicId")
    Optional<Resource> findFirstByPublicIdIncludingCachedUrls(URI publicId);
}
