/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.Optional;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.TaxonomyRepository;

public interface DomainEntityHelperService {
    Node getNodeByPublicId(URI publicId);

    DomainEntity getEntityByPublicId(URI publicId);

    TaxonomyRepository<DomainEntity> getRepository(URI publicId);

    void buildPathsForEntity(URI publicId);

    void deleteEntityByPublicId(URI publicId);

    Optional<DomainEntity> getProcessedEntityByPublicId(URI publicId, boolean addIsPublishing, boolean cleanUp);

    Optional<DomainEntity> updateEntity(DomainEntity domainEntity, boolean cleanUp);
}
