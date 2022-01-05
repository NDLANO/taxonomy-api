/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.CachedPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;

public interface CachedPathRepository extends JpaRepository<CachedPath, String> {

    @Query("SELECT cp from CachedPath cp LEFT JOIN FETCH cp.node n LEFT JOIN FETCH cp.resource r "
            + "WHERE cp.active = true AND (n.publicId = :publicId OR r.publicId = :publicId)")
    List<CachedPath> findAllByPublicId(URI publicId);

    int deleteByActiveFalse();
}
