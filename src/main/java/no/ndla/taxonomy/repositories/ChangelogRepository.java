/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import java.util.Optional;
import no.ndla.taxonomy.domain.Changelog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChangelogRepository extends JpaRepository<Changelog, Integer> {
    Optional<Changelog> findFirstByDoneFalse();

    @Modifying(flushAutomatically = true)
    @Query("delete Changelog c where c.done = true")
    int deleteByDoneTrue();
}
