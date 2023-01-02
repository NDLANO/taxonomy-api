/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.TaxonomyRepository;

import java.net.URI;

public interface DomainEntityHelperService {
    Node getNodeByPublicId(URI publicId);

    DomainEntity getEntityByPublicId(URI publicId);

    TaxonomyRepository<DomainEntity> getRepository(URI publicId);

    void buildPathsForEntity(URI publicId);

    void deleteEntityByPublicId(URI publicId);
}
