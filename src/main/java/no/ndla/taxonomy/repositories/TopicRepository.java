/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Topic;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface TopicRepository extends TaxonomyRepository<Topic> {
    @Query("SELECT DISTINCT t" + "   FROM Topic t" + "   LEFT JOIN FETCH t.cachedPaths"
            + "   LEFT JOIN FETCH t.translations" + "   WHERE t.context = :context")
    List<Topic> findAllByContextIncludingCachedUrlsAndTranslations(boolean context);

    @Query("SELECT DISTINCT t" + "   FROM Topic t" + "   LEFT JOIN FETCH t.cachedPaths"
            + "   LEFT JOIN FETCH t.translations")
    List<Topic> findAllIncludingCachedUrlsAndTranslations();

    @Query("SELECT DISTINCT t" + "   FROM Topic t" + "   LEFT JOIN FETCH t.cachedPaths"
            + "   LEFT JOIN FETCH t.translations" + "   WHERE t.publicId = :publicId")
    Optional<Topic> findFirstByPublicIdIncludingCachedUrlsAndTranslations(URI publicId);

    @Query("SELECT DISTINCT t" + "   FROM Topic t" + "   LEFT JOIN FETCH t.cachedPaths"
            + "   LEFT JOIN FETCH t.translations" + "   WHERE t.contentUri = :contentUri")
    List<Topic> findAllByContentUriIncludingCachedUrlsAndTranslations(URI contentUri);

    @Query("SELECT DISTINCT t" + "   FROM Topic t" + "   WHERE t.publicId = :publicId")
    Optional<Topic> findFirstByPublicIdIncludingFilters(URI publicId);

    @Query("SELECT DISTINCT t" + "   FROM Topic t" + "   LEFT JOIN FETCH t.cachedPaths"
            + "   WHERE t.publicId = :publicId")
    Optional<Topic> findFirstByPublicIdIncludingCachedUrls(URI publicId);

    Optional<Topic> findFirstByPublicId(URI publicId);
}
