/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Component
public class DomainEntityHelperServiceImpl implements DomainEntityHelperService {
    private final NodeRepository nodeRepository;
    private final ResourceRepository resourceRepository;

    public DomainEntityHelperServiceImpl(NodeRepository nodeRepository, ResourceRepository resourceRepository) {
        this.nodeRepository = nodeRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Node getNodeByPublicId(URI publicId) {
        return nodeRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node", publicId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public EntityWithPath getEntityByPublicId(URI publicId) {
        switch (publicId.getSchemeSpecificPart().split(":")[0]) {
        case "subject":
            return nodeRepository.findFirstByPublicId(publicId)
                    .orElseThrow(() -> new NotFoundServiceException("Subject by id was not found"));
        case "topic":
            return nodeRepository.findFirstByPublicId(publicId)
                    .orElseThrow(() -> new NotFoundServiceException("Topic by id was not found"));
        case "node":
            return nodeRepository.findFirstByPublicId(publicId)
                    .orElseThrow(() -> new NotFoundServiceException("Node by id was not found"));
        case "resource":
            return resourceRepository.findFirstByPublicId(publicId)
                    .orElseThrow(() -> new NotFoundServiceException("Resource by id was not found"));
        }

        throw new NotFoundServiceException("Unknown entity requested");
    }

}
