/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ResourceFetcher extends VersionSchemaFetcher<Resource> {

    @Autowired
    ResourceRepository resourceRepository;

    @Override
    protected Optional<Resource> callInternal() {
        return resourceRepository.fetchResourceGraphByPublicId(this.publicId);
    }
}
