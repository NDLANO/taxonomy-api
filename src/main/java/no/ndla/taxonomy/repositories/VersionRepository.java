/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface VersionRepository extends TaxonomyRepository<Version> {
    Optional<Version> findFirstByPublicId(URI publicId);

    Optional<Version> findFirstByHash(String hash);

    List<Version> findByVersionType(VersionType versionType);

    Optional<Version> findFirstByVersionType(VersionType versionType);
}
