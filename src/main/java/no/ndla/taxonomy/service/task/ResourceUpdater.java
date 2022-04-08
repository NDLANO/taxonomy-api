/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceTranslation;
import no.ndla.taxonomy.repositories.CustomFieldRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Component
public class ResourceUpdater extends VersionSchemaUpdater<Resource> {

    @Autowired
    CustomFieldRepository customFieldRepository;

    @Autowired
    ResourceRepository resourceRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Optional<Resource> callInternal() {
        Resource updated;
        ensureMetadataRefsExist(this.element.getMetadata(), customFieldRepository);
        Optional<Resource> existing = resourceRepository.fetchResourceGraphByPublicId(this.element.getPublicId());
        if (existing.isEmpty()) {
            // Resource is new
            Resource resource = new Resource(this.element);
            updated = resourceRepository.save(resource);
        } else {
            // Resource exists, update current
            Resource present = existing.get();
            present.setName(this.element.getName());
            present.setContentUri(this.element.getContentUri());
            mergeMetadata(present, this.element.getMetadata());
            mergeTranslations(present, this.element.getTranslations());
            updated = resourceRepository.save(present);
        }
        return Optional.of(updated);
    }

    private void mergeTranslations(Resource present, Set<ResourceTranslation> translations) {
    }
}
