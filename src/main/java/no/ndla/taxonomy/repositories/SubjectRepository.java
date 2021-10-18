/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Subject;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends TaxonomyRepository<Subject> {
    @Query(
            "SELECT DISTINCT s"
                    + "   FROM Subject s"
                    + "   LEFT JOIN FETCH s.cachedPaths"
                    + "   LEFT JOIN FETCH s.translations")
    List<Subject> findAllIncludingCachedUrlsAndTranslations();

    @Query(
            "SELECT DISTINCT s"
                    + "   FROM Subject s"
                    + "   LEFT JOIN FETCH s.cachedPaths"
                    + "   LEFT JOIN FETCH s.translations"
                    + "   WHERE s.publicId = :publicId")
    Optional<Subject> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    Optional<Subject> findFirstByPublicId(URI publicId);

    @Query(
            "SELECT DISTINCT s"
                    + "   FROM Subject s"
                    + "   LEFT JOIN FETCH s.cachedPaths"
                    + "   WHERE s.publicId = :publicId")
    Optional<Subject> findFirstByPublicIdIncludingCachedUrls(URI publicId);

    boolean existsByPublicId(URI publicId);
}
