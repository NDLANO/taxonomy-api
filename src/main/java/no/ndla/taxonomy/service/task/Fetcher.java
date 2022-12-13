/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.CustomField;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.EntityWithMetadata;
import no.ndla.taxonomy.domain.Metadata;
import no.ndla.taxonomy.service.CustomFieldService;
import no.ndla.taxonomy.service.DomainEntityHelperService;
import no.ndla.taxonomy.service.exceptions.EntityNotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

public class Fetcher extends Task<DomainEntity> {
    private DomainEntityHelperService domainEntityHelperService;
    private CustomFieldService customFieldService;
    private URI publicId;
    private boolean addIsPublishing;

    public Fetcher() {
    }

    public void setDomainEntityHelperService(DomainEntityHelperService domainEntityHelperService) {
        this.domainEntityHelperService = domainEntityHelperService;
    }

    public void setCustomFieldService(CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }

    public void setAddIsPublishing(boolean addIsPublishing) {
        this.addIsPublishing = addIsPublishing;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Optional<DomainEntity> execute() {
        // TaxonomyRepository<DomainEntity> repository = domainEntityHelperService.getRepository(this.publicId);
        DomainEntity entity = domainEntityHelperService.getEntityByPublicId(this.publicId);
        if (entity instanceof EntityWithMetadata) {
            EntityWithMetadata entityWithMetadata = (EntityWithMetadata) entity;
            if (addIsPublishing && !cleanUp) {
                if (!entityWithMetadata.getMetadata().getCustomFieldValues().stream()
                        .map(customFieldValue -> customFieldValue.getCustomField().getKey())
                        .collect(Collectors.toList()).contains(CustomField.IS_PUBLISHING)) {
                    customFieldService.setCustomField(entityWithMetadata.getMetadata(), CustomField.IS_PUBLISHING,
                            "true");
                }
                return Optional.of(entity);
            }
            if (cleanUp) {
                Metadata metadata = entityWithMetadata.getMetadata();
                unsetCustomField(metadata, CustomField.IS_PUBLISHING);
                unsetCustomField(metadata, CustomField.REQUEST_PUBLISH);
                unsetCustomField(metadata, CustomField.IS_CHANGED);
            }
        }
        return Optional.ofNullable(entity);
    }

    private void unsetCustomField(Metadata metadata, String customfield) {
        metadata.getCustomFieldValues().forEach(customFieldValue -> {
            if (customFieldValue.getCustomField().getKey().equals(customfield)) {
                try {
                    customFieldService.unsetCustomField(customFieldValue.getId());
                } catch (EntityNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
