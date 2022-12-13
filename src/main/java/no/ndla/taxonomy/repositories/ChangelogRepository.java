/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Changelog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChangelogRepository extends JpaRepository<Changelog, Integer> {
    Optional<Changelog> findFirstByDoneFalse();

    int deleteByDoneTrue();
}
