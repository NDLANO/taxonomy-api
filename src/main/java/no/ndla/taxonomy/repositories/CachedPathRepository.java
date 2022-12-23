/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.CachedPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.net.URI;
import java.util.List;

public interface CachedPathRepository extends JpaRepository<CachedPath, String> {

    @Query("select cp from CachedPath cp where cp.active = true and cp.publicId = ?1")
    List<CachedPath> findAllByPublicId(URI publicId);

    @Modifying(flushAutomatically = true)
    @Query("delete CachedPath c where c.active = false")
    int deleteByActiveFalse();
}
