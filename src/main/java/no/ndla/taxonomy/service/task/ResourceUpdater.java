/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ResourceUpdater extends VersionSchemaUpdater<Resource> {

    @Autowired
    CustomFieldRepository customFieldRepository;

    @Autowired
    ResourceRepository resourceRepository;

    @Autowired
    CachedUrlUpdaterService cachedUrlUpdaterService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Optional<Resource> callInternal() {
        Resource updated;
        ensureMetadataRefsExist(this.element.getMetadata());
        Optional<Resource> existing = resourceRepository.fetchRepositoryGraphByPublicId(this.element.getPublicId());
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
            updated = resourceRepository.save(present);
        }
        // cachedUrlUpdaterService.updateCachedUrls(updated);
        return Optional.of(updated);
    }

    private void ensureMetadataRefsExist(Metadata metadata) {
        if (!metadata.getCustomFieldValues().isEmpty()) {
            metadata.getCustomFieldValues().forEach(customFieldValue -> {
                CustomField toSave = customFieldValue.getCustomField();
                CustomField saved;
                Optional<CustomField> byKey = customFieldRepository.findByKey(toSave.getKey());
                if (byKey.isPresent()) {
                    // Same customField, do nothing
                    saved = byKey.get();
                } else {
                    toSave.setId(null);
                    saved = customFieldRepository.save(toSave);
                }
                customFieldValue.setCustomField(saved);
            });
        }
    }
}
