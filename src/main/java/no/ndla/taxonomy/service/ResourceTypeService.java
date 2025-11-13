/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.persistence.EntityManager;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResourceTypeService {
    private final EntityManager entityManager;
    private final ResourceTypeRepository resourceTypeRepository;

    public ResourceTypeService(EntityManager entityManager, ResourceTypeRepository resourceTypeRepository) {
        this.entityManager = entityManager;
        this.resourceTypeRepository = resourceTypeRepository;
    }

    @Transactional
    public void shiftOrderAfterInsert(ResourceType resourceType) {
        var allResourceTypes = resourceTypeRepository.findAllByOrderByOrderAsc();
        if (resourceType.getOrder() == -1) {
            resourceType.setOrder(allResourceTypes.size());
            entityManager.merge(resourceType);
        }
        for (var rt : allResourceTypes) {
            if (rt.getOrder() >= resourceType.getOrder() && !rt.getPublicId().equals(resourceType.getPublicId())) {
                rt.setOrder(rt.getOrder() + 1);
                entityManager.merge(rt);
            }
        }
    }

    @Transactional
    public void updateOrderAfterDelete() {
        var allResourceTypes = resourceTypeRepository.findAllByOrderByOrderAsc();
        for (var i = 0; i < allResourceTypes.size(); i++) {
            var rt = allResourceTypes.get(i);
            rt.setOrder(i);
            entityManager.merge(rt);
        }
    }
}
