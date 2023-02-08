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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class DomainEntityHelperServiceImpl implements DomainEntityHelperService {
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;

    public DomainEntityHelperServiceImpl(NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository, ResourceTypeRepository resourceTypeRepository,
            CachedUrlUpdaterService cachedUrlUpdaterService) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Node getNodeByPublicId(URI publicId) {
        return nodeRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node", publicId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public DomainEntity getEntityByPublicId(URI publicId) {
        switch (publicId.getSchemeSpecificPart().split(":")[0]) {
        case "subject":
        case "topic":
        case "node":
        case "resource":
            return nodeRepository.findFirstByPublicId(publicId).orElse(null);
        case "node-connection":
        case "subject-topic":
        case "topic-subtopic":
        case "node-resource":
        case "topic-resource":
            return nodeConnectionRepository.findByPublicId(publicId);
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

    @Override
    @Transactional
    public Optional<DomainEntity> getProcessedEntityByPublicId(URI publicId, boolean addIsPublishing, boolean cleanUp) {
        DomainEntity entity = getEntityByPublicId(publicId);
        if (entity != null) {
            initializeFields(entity);
        }
        if (entity instanceof EntityWithMetadata entityWithMetadata) {
            if (addIsPublishing && !cleanUp) {
                // TODO: Her var det en rar ifstatement, trenger man den?
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
        TaxonomyRepository<DomainEntity> repository = getRepository(node.getPublicId());
        Node existing = (Node) getEntityByPublicId(node.getPublicId());
        node.getResourceTypes().forEach(this::ensureResourceTypesExists);
        if (existing == null) {
            mergeMetadata(null, node.getMetadata(), node.getPublicId(), cleanUp);
            result = repository.save(new Node(node));
        } else if (existing.equals(node)) {
            result = existing;
        } else {
            logger.debug("Updating node " + node.getPublicId());
            existing.setName(node.getName());
            existing.setContentUri(node.getContentUri());
            existing.setNodeType(node.getNodeType());
            existing.setRoot(node.isRoot());
            existing.setContext(node.isContext());

            // ResourceTypes
            Collection<URI> typesToSet = new HashSet<>();
            Collection<URI> typesToKeep = new HashSet<>();
            List<URI> existingTypes = existing.getResourceTypes().stream().map(DomainEntity::getPublicId)
                    .collect(Collectors.toList());
            node.getResourceTypes().forEach(resourceType -> {
                if (existingTypes.contains(resourceType.getPublicId())) {
                    typesToKeep.add(resourceType.getPublicId());
                } else {
                    typesToSet.add(resourceType.getPublicId());
                }
            });
            if (!typesToSet.isEmpty()) {
                Map<URI, URI> reusedUris = node.getResourceResourceTypes().stream()
                        .filter(resourceResourceType -> typesToSet
                                .contains(resourceResourceType.getResourceType().getPublicId()))
                        .collect(Collectors.toMap(
                                resourceResourceType -> resourceResourceType.getResourceType().getPublicId(),
                                ResourceResourceType::getPublicId));

                Collection<ResourceResourceType> toRemove = new HashSet<>();
                existing.getResourceResourceTypes().forEach(resourceResourceType -> {
                    if (!typesToKeep.contains(resourceResourceType.getResourceType().getPublicId())) {
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

            // Translations
            Set<JsonTranslation> translations = new HashSet<>();
            for (JsonTranslation nodeTranslation : node.getTranslations()) {
                var existingTranslation = existing.getTranslations().stream()
                        .filter(translation -> translation.getLanguageCode().equals(nodeTranslation.getLanguageCode()))
                        .findFirst();
                if (existingTranslation.isPresent()) {
                    var tr = existingTranslation.get();
                    tr.setName(nodeTranslation.getName());
                    translations.add(tr);
                } else {
                    translations.add(new JsonTranslation(nodeTranslation));
                }
            }
            if (!translations.isEmpty()) {
                existing.clearTranslations();
                for (var translation : translations) {
                    existing.addTranslation(translation);
                }
            }

            // Metadata
            mergeMetadata(existing, node.getMetadata(), node.getPublicId(), cleanUp);
            result = repository.save(existing);

            // delete orphans
            List<URI> childIds = node.getChildren().stream().map(DomainEntity::getPublicId)
                    .collect(Collectors.toList());
            result.getChildren().forEach(nodeConnection -> {
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
        TaxonomyRepository<DomainEntity> repository = getRepository(nodeConnection.getPublicId());
        TaxonomyRepository<DomainEntity> nodeRepository = getRepository(URI.create("urn:node:dummy"));

        NodeConnection existing = (NodeConnection) getEntityByPublicId(nodeConnection.getPublicId());
        if (existing == null) {
            mergeMetadata(null, nodeConnection.getMetadata(), nodeConnection.getPublicId(), cleanUp);
            // Use correct objects when copying
            nodeConnection
                    .setParent((Node) nodeRepository.findByPublicId(nodeConnection.getParent().get().getPublicId()));
            nodeConnection
                    .setChild((Node) nodeRepository.findByPublicId(nodeConnection.getChild().get().getPublicId()));
            NodeConnection connection = new NodeConnection(nodeConnection);
            connection.setPublicId(nodeConnection.getPublicId());
            result = repository.save(connection);
        } else {
            /*
             * if (existing.equals(nodeConnection)) { return Optional.of(existing); }
             */
            logger.debug("Updating nodeconnection " + nodeConnection.getPublicId());
            existing.setParent((Node) nodeRepository.findByPublicId(nodeConnection.getParent().get().getPublicId()));
            existing.setChild((Node) nodeRepository.findByPublicId(nodeConnection.getChild().get().getPublicId()));
            existing.setRank(nodeConnection.getRank());
            if (nodeConnection.getRelevance().isPresent()) {
                existing.setRelevance(nodeConnection.getRelevance().get());
            }
            if (nodeConnection.isPrimary().isPresent()) {
                existing.setPrimary(nodeConnection.isPrimary().get());
            }

            mergeMetadata(existing, nodeConnection.getMetadata(), nodeConnection.getPublicId(), cleanUp);
            result = repository.save(existing);
        }
        if (cleanUp) {
            buildPathsForEntity(result.getChild().get().getPublicId());
        }
        return Optional.of(result);
    }

    private ResourceType ensureResourceTypesExists(ResourceType resourceType) {
        ResourceType parent = null;
        if (resourceType.getParent().isPresent()) {
            parent = ensureResourceTypesExists(resourceType.getParent().get());
        }
        Optional<ResourceType> existing = resourceTypeRepository
                .findFirstByPublicIdIncludingTranslations(resourceType.getPublicId());
        if (existing.isEmpty()) {
            // Create resource type
            ResourceType rt = new ResourceType(resourceType, parent);
            return resourceTypeRepository.save(rt);
        }
        return existing.get();
    }

    protected void mergeMetadata(EntityWithMetadata present, Metadata metadata, URI publicId, boolean cleanUp) {
        Metadata presentMetadata = present.getMetadata();
        if (presentMetadata.equals(metadata)) {
            // All ok, do nothing
            return;
        }

        presentMetadata.setVisible(metadata.isVisible());
        for (var grepCode : metadata.getGrepCodes()) {
            if (!presentMetadata.getGrepCodes().stream().map(JsonGrepCode::code).toList().contains(grepCode.code())) {
                presentMetadata.addGrepCode(new JsonGrepCode(grepCode));
            }
        }
        if (!cleanUp) {
            metadata.getCustomFields().forEach((key, value) -> {
                if (!List.of(CustomField.IS_PUBLISHING, CustomField.IS_CHANGED, CustomField.REQUEST_PUBLISH)
                        .contains(key)) {
                    present.setCustomField(key, value);
                }
            });
        } else {
            logger.debug("Cleaning metadata in updater");
            presentMetadata.getCustomFields().entrySet().forEach(customFieldValue -> {
                var key = customFieldValue.getKey();
                // Remove specially handled fields
                if (Arrays.asList(CustomField.IS_PUBLISHING, CustomField.IS_CHANGED, CustomField.REQUEST_PUBLISH)
                        .contains(key)) {
                    present.unsetCustomField(customFieldValue.getKey());
                    if (key.equals(CustomField.IS_PUBLISHING)) {
                        logger.info(String.format("Publishing of node %s finished", publicId));
                    }
                }
            });
        }
    }
}
