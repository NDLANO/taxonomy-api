/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class DomainEntityHelperServiceImpl implements DomainEntityHelperService {
    private final NodeRepository nodeRepository;
    private final ResourceRepository resourceRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeResourceRepository nodeResourceRepository;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;

    public DomainEntityHelperServiceImpl(NodeRepository nodeRepository, ResourceRepository resourceRepository,
            NodeConnectionRepository nodeConnectionRepository, NodeResourceRepository nodeResourceRepository,
            CachedUrlUpdaterService cachedUrlUpdaterService) {
        this.nodeRepository = nodeRepository;
        this.resourceRepository = resourceRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeResourceRepository = nodeResourceRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Node getNodeByPublicId(URI publicId) {
        return nodeRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node", publicId));
    }

    @Override
    @Transactional
    public DomainEntity getEntityByPublicId(URI publicId) {
        switch (publicId.getSchemeSpecificPart().split(":")[0]) {
        case "subject":
        case "topic":
        case "node":
            return nodeRepository.findNodeGraphByPublicId(publicId);
        case "resource":
            return resourceRepository.findResourceGraphByPublicId(publicId);
        case "node-connection":
        case "subject-topic":
        case "topic-subtopic":
            return nodeConnectionRepository.findByPublicId(publicId);
        case "node-resource":
        case "topic-resource":
            return nodeResourceRepository.findByPublicId(publicId);
        }
        throw new NotFoundServiceException("Entity of type not found");
    }

    @Override
    public TaxonomyRepository getRepository(URI publicId) {
        switch (publicId.getSchemeSpecificPart().split(":")[0]) {
        case "subject":
        case "topic":
        case "node":
            return nodeRepository;
        case "resource":
            return resourceRepository;
        case "node-connection":
        case "subject-topic":
        case "topic-subtopic":
            return nodeConnectionRepository;
        case "node-resource":
        case "topic-resource":
            return nodeResourceRepository;
        }
        throw new NotFoundServiceException(String.format("Unknown repository requested: %s", publicId));
    }

    @Override
    @Transactional
    public void buildPathsForEntity(URI publicId) {
        DomainEntity domainEntity = getEntityByPublicId(publicId);
        if (domainEntity instanceof EntityWithPath) {
            cachedUrlUpdaterService.updateCachedUrls((EntityWithPath) domainEntity);
        }
    }

    @Override
    @Transactional
    public void deleteEntityByPublicId(URI publicId) {
        TaxonomyRepository repository = getRepository(publicId);
        repository.deleteByPublicId(publicId);
    }
}
