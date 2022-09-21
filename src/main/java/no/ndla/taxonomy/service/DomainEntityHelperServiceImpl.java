/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithMetadata;
import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.NodeResourceRepository;
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
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeResourceRepository nodeResourceRepository;

    public DomainEntityHelperServiceImpl(NodeRepository nodeRepository, ResourceRepository resourceRepository,
            NodeConnectionRepository nodeConnectionRepository, NodeResourceRepository nodeResourceRepository) {
        this.nodeRepository = nodeRepository;
        this.resourceRepository = resourceRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeResourceRepository = nodeResourceRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Node getNodeByPublicId(URI publicId) {
        return nodeRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node", publicId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public EntityWithMetadata getEntityByPublicId(URI publicId) {
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
        case "node-connection":
        case "subject-topic":
        case "topic-subtopic":
            return nodeConnectionRepository.findFirstByPublicId(publicId)
                    .orElseThrow(() -> new NotFoundServiceException("NodeConnection by id was not found"));
        case "node-resource":
        case "topic-resource":
            return nodeResourceRepository.findFirstByPublicId(publicId)
                    .orElseThrow(() -> new NotFoundServiceException("NodeResource by id was not found"));
        }

        throw new NotFoundServiceException("Unknown entity requested");
    }

}
