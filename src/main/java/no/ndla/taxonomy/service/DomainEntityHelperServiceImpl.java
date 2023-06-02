/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DomainEntityHelperServiceImpl implements DomainEntityHelperService {
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final ContextUpdaterService cachedUrlUpdaterService;

    public DomainEntityHelperServiceImpl(
            NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository,
            ResourceTypeRepository resourceTypeRepository,
            ContextUpdaterService cachedUrlUpdaterService) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Node getNodeByPublicId(URI publicId) {
        return nodeRepository
                .findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node", publicId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public DomainEntity getEntityByPublicId(URI publicId) {
        switch (publicId.getSchemeSpecificPart().split(":")[0]) {
            case "subject", "topic", "node", "resource" -> {
                return nodeRepository.findFirstByPublicId(publicId).orElse(null);
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
        if (domainEntity instanceof Node) {
            cachedUrlUpdaterService.updateContexts((Node) domainEntity);
        }
    }

    @Override
    @Transactional
    public void deleteEntityByPublicId(URI publicId) {
        TaxonomyRepository repository = getRepository(publicId);
        repository.deleteByPublicId(publicId);
    }

    @Override
    @Transactional
    public Optional<DomainEntity> getProcessedEntityByPublicId(URI publicId, boolean addIsPublishing, boolean cleanUp) {
        DomainEntity entity = getEntityByPublicId(publicId);
        if (entity != null) {
            initializeFields(entity);
        }
        if (entity instanceof EntityWithMetadata entityWithMetadata) {
            if (addIsPublishing && !cleanUp) {
                entityWithMetadata.setCustomField(CustomField.IS_PUBLISHING, "true");
                return Optional.of(entity);
            }
            if (cleanUp) {
                entityWithMetadata.unsetCustomField(CustomField.IS_PUBLISHING);
                entityWithMetadata.unsetCustomField(CustomField.REQUEST_PUBLISH);
                entityWithMetadata.unsetCustomField(CustomField.IS_CHANGED);
            }
        }
        return Optional.ofNullable(entity);
    }

    private void initializeFields(DomainEntity domainEntity) {
        if (domainEntity instanceof Node) {
            ((Node) domainEntity).getTranslations().forEach(JsonTranslation::getName);
            ((Node) domainEntity).getChildConnections();
            ((Node) domainEntity).getParentConnections();
            ((Node) domainEntity).getResourceResourceTypes();
            ((Node) domainEntity).getResourceResourceTypes().forEach(ResourceResourceType::getResourceType);
            ((Node) domainEntity).getResourceTypes().forEach(this::initializeFields);
        } else if (domainEntity instanceof NodeConnection) {
            ((NodeConnection) domainEntity).getParent().ifPresent(this::initializeFields);
            ((NodeConnection) domainEntity).getChild().ifPresent(this::initializeFields);
        } else if (domainEntity instanceof ResourceType) {
            ((ResourceType) domainEntity).getParent().ifPresent(this::initializeFields);
            ((ResourceType) domainEntity).getTranslations().forEach(JsonTranslation::getName);
        }
    }

    @Override
    @Transactional
    public Optional<DomainEntity> updateEntity(DomainEntity domainEntity, boolean cleanUp) {
        if (domainEntity instanceof Node) {
            return updateNode((Node) domainEntity, cleanUp);
        }
        if (domainEntity instanceof NodeConnection) {
            return updateNodeConnection((NodeConnection) domainEntity, cleanUp);
        }
        throw new IllegalArgumentException("Wrong type of element to update: " + domainEntity.getEntityName());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    private Optional<DomainEntity> updateNode(Node node, boolean cleanUp) {
        Node result;
        Node existing = nodeRepository.findByPublicId(node.getPublicId());
        node.getResourceTypes().forEach(this::ensureResourceTypesExists);
        if (existing == null) {
            result = nodeRepository.save(new Node(node));
        } else if (existing.equals(node)) {
            result = existing;
        } else {
            logger.debug("Updating node " + node.getPublicId());
            existing.setName(node.getName());
            existing.setContentUri(node.getContentUri());
            existing.setNodeType(node.getNodeType());
            existing.setContext(node.isContext());
            existing.setTranslations(node.getTranslations());
            existing.setVisible(node.isVisible());
            existing.setCustomFields(node.getCustomFields());
            existing.setGrepCodes(node.getGrepCodes());

            // ResourceTypes
            Collection<URI> typesToSet = new HashSet<>();
            Collection<URI> typesToKeep = new HashSet<>();
            List<URI> existingTypes = existing.getResourceTypes().stream()
                    .map(DomainEntity::getPublicId)
                    .toList();
            node.getResourceTypes().forEach(resourceType -> {
                if (existingTypes.contains(resourceType.getPublicId())) {
                    typesToKeep.add(resourceType.getPublicId());
                } else {
                    typesToSet.add(resourceType.getPublicId());
                }
            });
            if (!typesToSet.isEmpty()) {
                Map<URI, URI> reusedUris = node.getResourceResourceTypes().stream()
                        .filter(resourceResourceType -> typesToSet.contains(
                                resourceResourceType.getResourceType().getPublicId()))
                        .collect(Collectors.toMap(
                                resourceResourceType ->
                                        resourceResourceType.getResourceType().getPublicId(),
                                ResourceResourceType::getPublicId));

                Collection<ResourceResourceType> toRemove = new HashSet<>();
                existing.getResourceResourceTypes().forEach(resourceResourceType -> {
                    if (!typesToKeep.contains(
                            resourceResourceType.getResourceType().getPublicId())) {
                        toRemove.add(resourceResourceType);
                    }
                });
                toRemove.forEach(existing::removeResourceResourceType);
                typesToSet.forEach(uri -> {
                    ResourceType resourceType = resourceTypeRepository.findByPublicId(uri);
                    ResourceResourceType resourceResourceType = existing.addResourceType(resourceType);
                    if (reusedUris.containsKey(resourceType.getPublicId())) {
                        resourceResourceType.setPublicId(reusedUris.get(resourceType.getPublicId()));
                    }
                });
            }

            result = nodeRepository.save(existing);

            // delete orphans
            List<URI> childIds = node.getChildConnections().stream()
                    .map(DomainEntity::getPublicId)
                    .toList();
            result.getChildConnections().forEach(nodeConnection -> {
                if (!childIds.contains(nodeConnection.getPublicId())) {
                    // Connection deleted
                    deleteEntityByPublicId(nodeConnection.getPublicId());
                }
            });
        }
        if (cleanUp) {
            buildPathsForEntity(result.getPublicId());
        }
        return Optional.of(result);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    private Optional<DomainEntity> updateNodeConnection(NodeConnection nodeConnection, boolean cleanUp) {
        NodeConnection result;
        NodeConnection existing = nodeConnectionRepository.findByPublicId(nodeConnection.getPublicId());
        if (existing == null) {
            // Check if connection have changed id
            Node parent = nodeRepository.findByPublicId(
                    nodeConnection.getParent().get().getPublicId());
            Node child = nodeRepository.findByPublicId(
                    nodeConnection.getChild().get().getPublicId());
            existing = nodeConnectionRepository.findByParentIdAndChildId(parent.getId(), child.getId());
            if (existing != null) {
                existing.setPublicId(nodeConnection.getPublicId());
                result = updateExisting(existing, nodeConnection);
            } else {
                nodeConnection.setParent(parent);
                nodeConnection.setChild(child);
                NodeConnection connection = new NodeConnection(nodeConnection);
                result = nodeConnectionRepository.save(connection);
            }
        } else {
            if (existing.equals(nodeConnection)) {
                return Optional.of(existing);
            }
            logger.debug("Updating nodeconnection " + nodeConnection.getPublicId());
            existing.setParent(nodeRepository.findByPublicId(
                    nodeConnection.getParent().get().getPublicId()));
            existing.setChild(nodeRepository.findByPublicId(
                    nodeConnection.getChild().get().getPublicId()));
            result = updateExisting(existing, nodeConnection);
        }
        if (cleanUp) {
            buildPathsForEntity(result.getChild().get().getPublicId());
        }
        return Optional.of(result);
    }

    private NodeConnection updateExisting(NodeConnection existing, NodeConnection nodeConnection) {
        existing.setRank(nodeConnection.getRank());
        existing.setVisible(nodeConnection.isVisible());
        existing.setGrepCodes(nodeConnection.getGrepCodes());
        existing.setCustomFields(nodeConnection.getCustomFields());
        if (nodeConnection.getRelevance().isPresent()) {
            existing.setRelevance(nodeConnection.getRelevance().get());
        }
        if (nodeConnection.isPrimary().isPresent()) {
            existing.setPrimary(nodeConnection.isPrimary().get());
        }
        return nodeConnectionRepository.save(existing);
    }

    private ResourceType ensureResourceTypesExists(ResourceType resourceType) {
        ResourceType parent = null;
        if (resourceType.getParent().isPresent()) {
            parent = ensureResourceTypesExists(resourceType.getParent().get());
        }
        Optional<ResourceType> existing =
                resourceTypeRepository.findFirstByPublicIdIncludingTranslations(resourceType.getPublicId());
        if (existing.isEmpty()) {
            // Create resource type
            ResourceType rt = new ResourceType(resourceType, parent);
            return resourceTypeRepository.save(rt);
        }
        return existing.get();
    }
}
