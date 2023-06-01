/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import no.ndla.taxonomy.domain.Relevance;
import org.springframework.data.jpa.repository.Query;

public interface RelevanceRepository extends TaxonomyRepository<Relevance> {
    @Query("SELECT r FROM Relevance r")
    Set<Relevance> findAllIncludingTranslations();

    @Query("SELECT r FROM Relevance r WHERE r.publicId = :publicId")
    Optional<Relevance> findFirstByPublicIdIncludingTranslations(URI publicId);
}
