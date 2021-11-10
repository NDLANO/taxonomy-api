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
import java.util.Optional;
import java.util.Set;

public interface VersionRepository extends TaxonomyRepository<Version> {
    Optional<Version> findFirstByPublicId(URI publicId);

    Optional<Version> findByVersionType(VersionType versionType);
}
