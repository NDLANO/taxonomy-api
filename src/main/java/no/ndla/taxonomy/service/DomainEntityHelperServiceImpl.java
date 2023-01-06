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
    private final NodeConnectionRepository nodeConnectionRepository;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;

    public DomainEntityHelperServiceImpl(
            NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository,
            CachedUrlUpdaterService cachedUrlUpdaterService
    ) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
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
            case "subject", "topic", "node", "resource" -> {
                return nodeRepository.findNodeGraphByPublicId(publicId);
            }
            case "node-connection", "subject-topic", "topic-subtopic", "node-resource", "topic-resource" -> {
                return nodeConnectionRepository.findByPublicId(publicId);
            }
        }
        throw new NotFoundServiceException("Entity of type not found");
    }

    @Override
    public TaxonomyRepository getRepository(URI publicId) {
        switch (publicId.getSchemeSpecificPart().split(":")[0]) {
            case "subject", "topic", "node", "resource" -> {
                return nodeRepository;
            }
            case "node-connection", "subject-topic", "topic-subtopic", "node-resource", "topic-resource" -> {
                return nodeConnectionRepository;
            }
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
