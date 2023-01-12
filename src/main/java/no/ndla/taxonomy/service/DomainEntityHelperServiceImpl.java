/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.exceptions.EntityNotFoundException;
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
    private final ResourceRepository resourceRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeResourceRepository nodeResourceRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;
    private final CustomFieldService customFieldService;

    public DomainEntityHelperServiceImpl(NodeRepository nodeRepository, ResourceRepository resourceRepository,
            NodeConnectionRepository nodeConnectionRepository, NodeResourceRepository nodeResourceRepository,
            ResourceTypeRepository resourceTypeRepository, CachedUrlUpdaterService cachedUrlUpdaterService,
            CustomFieldService customFieldService) {
        this.nodeRepository = nodeRepository;
        this.resourceRepository = resourceRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeResourceRepository = nodeResourceRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
        this.customFieldService = customFieldService;
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

    @Override
    @Transactional
    public Optional<DomainEntity> getProcessedEntityByPublicId(URI publicId, boolean addIsPublishing, boolean cleanUp) {
        DomainEntity entity = getEntityByPublicId(publicId);
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
                    // Already deleted. Do nothing.
                }
            }
        });
    }

    @Override
    @Transactional
    public Optional<DomainEntity> updateEntity(DomainEntity domainEntity, boolean cleanUp) {
        if (domainEntity instanceof Node) {
            return updateNode((Node) domainEntity, cleanUp);
        }
        if (domainEntity instanceof Resource) {
            return updateResource((Resource) domainEntity, cleanUp);
        }
        if (domainEntity instanceof NodeConnection) {
            return updateNodeConnection((NodeConnection) domainEntity, cleanUp);
        }
        if (domainEntity instanceof NodeResource) {
            return updateNodeResource((NodeResource) domainEntity, cleanUp);
        }
        throw new IllegalArgumentException("Wrong type of element to update: " + domainEntity.getEntityName());

    }

    @Transactional(propagation = Propagation.MANDATORY)
    private Optional<DomainEntity> updateNode(Node node, boolean cleanUp) {
        Node result;
        TaxonomyRepository<DomainEntity> repository = getRepository(node.getPublicId());
        Node existing = (Node) getEntityByPublicId(node.getPublicId());
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

            // Translations
            Set<NodeTranslation> translations = new HashSet<>();
            for (NodeTranslation nodeTranslation : node.getTranslations()) {
                Optional<NodeTranslation> existingTranslation = existing.getTranslations().stream()
                        .filter(translation -> translation.getLanguageCode().equals(nodeTranslation.getLanguageCode()))
                        .findFirst();
                if (existingTranslation.isPresent()) {
                    NodeTranslation tr = existingTranslation.get();
                    tr.setName(nodeTranslation.getName());
                    translations.add(tr);
                } else {
                    translations.add(new NodeTranslation(nodeTranslation, existing));
                }
            }
            if (!translations.isEmpty()) {
                existing.clearTranslations();
                for (NodeTranslation translation : translations) {
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
            List<URI> resources = node.getNodeResources().stream().map(DomainEntity::getPublicId)
                    .collect(Collectors.toList());
            result.getNodeResources().forEach(nodeResource -> {
                if (!resources.contains(nodeResource.getPublicId())) {
                    deleteEntityByPublicId(nodeResource.getPublicId());
                }
            });
        }
        if (cleanUp) {
            buildPathsForEntity(result.getPublicId());
        }
        return Optional.of(result);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    private Optional<DomainEntity> updateResource(Resource resource, boolean cleanUp) {
        Resource result;
        TaxonomyRepository<DomainEntity> repository = getRepository(resource.getPublicId());
        Resource existing = (Resource) getEntityByPublicId(resource.getPublicId());
        resource.getResourceTypes().forEach(this::ensureResourceTypesExists);
        if (existing == null) {
            mergeMetadata(null, resource.getMetadata(), resource.getPublicId(), cleanUp);
            result = repository.save(new Resource(resource, true));
        } else if (existing.equals(resource)) {
            logger.debug("Resource " + resource.getPublicId() + " is equal, continue");
            result = existing;
        } else {
            logger.debug("Updating resource " + resource.getPublicId());
            existing.setName(resource.getName());
            existing.setContentUri(resource.getContentUri());

            // ResourceTypes
            Collection<URI> typesToSet = new HashSet<>();
            Collection<URI> typesToKeep = new HashSet<>();
            List<URI> existingTypes = existing.getResourceTypes().stream().map(DomainEntity::getPublicId)
                    .collect(Collectors.toList());
            resource.getResourceTypes().forEach(resourceType -> {
                if (existingTypes.contains(resourceType.getPublicId())) {
                    typesToKeep.add(resourceType.getPublicId());
                } else {
                    typesToSet.add(resourceType.getPublicId());
                }
            });
            if (!typesToSet.isEmpty()) {
                Map<URI, URI> reusedUris = resource.getResourceResourceTypes().stream()
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
            Set<ResourceTranslation> translations = new HashSet<>();
            for (ResourceTranslation resourceTranslation : resource.getTranslations()) {
                Optional<ResourceTranslation> existingTranslation = existing.getTranslations().stream().filter(
                        translation -> translation.getLanguageCode().equals(resourceTranslation.getLanguageCode()))
                        .findFirst();
                if (existingTranslation.isPresent()) {
                    ResourceTranslation rt = existingTranslation.get();
                    rt.setName(resourceTranslation.getName());
                    translations.add(rt);
                } else {
                    translations.add(new ResourceTranslation(resourceTranslation, existing));
                }
            }
            if (!translations.isEmpty()) {
                existing.clearTranslations();
                for (ResourceTranslation translation : translations) {
                    existing.addTranslation(translation);
                }
            }

            // Metadata
            mergeMetadata(existing, resource.getMetadata(), resource.getPublicId(), cleanUp);
            result = repository.save(existing);
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

    @Transactional(propagation = Propagation.MANDATORY)
    private Optional<DomainEntity> updateNodeResource(NodeResource nodeResource, boolean cleanUp) {
        NodeResource result;
        TaxonomyRepository<DomainEntity> repository = getRepository(nodeResource.getPublicId());
        TaxonomyRepository<DomainEntity> nodeRepository = getRepository(URI.create("urn:node:dummy"));
        TaxonomyRepository<DomainEntity> resourceRepository = getRepository(URI.create("urn:resource:dummy"));

        NodeResource existing = (NodeResource) getEntityByPublicId(nodeResource.getPublicId());
        if (existing == null) {
            mergeMetadata(null, nodeResource.getMetadata(), nodeResource.getPublicId(), cleanUp);
            // Use correct objects when copying
            nodeResource.setNode((Node) nodeRepository.findByPublicId(nodeResource.getNode().get().getPublicId()));
            nodeResource.setResource(
                    (Resource) resourceRepository.findByPublicId(nodeResource.getResource().get().getPublicId()));
            NodeResource connection = new NodeResource(nodeResource);
            result = repository.save(connection);
        } else {
            /*
             * if (existing.equals(nodeResource)) { return Optional.of(existing); }
             */
            logger.debug("Updating noderesource " + nodeResource.getPublicId());
            existing.setNode((Node) nodeRepository.findByPublicId(nodeResource.getNode().get().getPublicId()));
            existing.setResource(
                    (Resource) resourceRepository.findByPublicId(nodeResource.getResource().get().getPublicId()));
            existing.setRank(nodeResource.getRank());
            if (nodeResource.getRelevance().isPresent()) {
                existing.setRelevance(nodeResource.getRelevance().get());
            }
            if (nodeResource.isPrimary().isPresent()) {
                existing.setPrimary(nodeResource.isPrimary().get());
            }
            mergeMetadata(existing, nodeResource.getMetadata(), nodeResource.getPublicId(), cleanUp);
            result = repository.save(existing);
        }
        if (cleanUp) {
            buildPathsForEntity(result.getResource().get().getPublicId());
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
        if (present == null) {
            // Make sure custom fields exists
            metadata.getCustomFieldValues().forEach(customFieldValue -> customFieldService.setCustomField(null,
                    customFieldValue.getCustomField().getKey(), null));
            return;
        }
        Metadata presentMetadata = present.getMetadata();
        if (presentMetadata.equals(metadata)) {
            // All ok, do nothing
            return;
        }

        presentMetadata.setVisible(metadata.isVisible());
        for (GrepCode grepCode : metadata.getGrepCodes()) {
            if (!presentMetadata.getGrepCodes().stream().map(GrepCode::getCode).collect(Collectors.toList())
                    .contains(grepCode.getCode())) {
                presentMetadata.addGrepCode(new GrepCode(grepCode, presentMetadata));
            }
        }
        if (!cleanUp) {
            List<String> existingKeys = presentMetadata.getCustomFieldValues().stream()
                    .map(customFieldValue1 -> customFieldValue1.getCustomField().getKey()).collect(Collectors.toList());
            metadata.getCustomFieldValues().forEach(customFieldValue -> {
                String key = customFieldValue.getCustomField().getKey();
                if (!List.of(CustomField.IS_PUBLISHING, CustomField.IS_CHANGED, CustomField.REQUEST_PUBLISH)
                        .contains(key)) {
                    if (!existingKeys.contains(key)) {
                        customFieldService.setCustomField(presentMetadata, key, customFieldValue.getValue());
                    } else {
                        // Update customfield
                        Optional<CustomFieldValue> value = presentMetadata.getCustomFieldValueByKey(key);
                        value.ifPresent(fieldValue -> fieldValue.setValue(customFieldValue.getValue()));
                    }
                }
            });
        } else {
            logger.debug("Cleaning metadata in updater");
            presentMetadata.getCustomFieldValues().forEach(customFieldValue -> {
                String key = customFieldValue.getCustomField().getKey();
                // Remove specially handled fields
                if (Arrays.asList(CustomField.IS_PUBLISHING, CustomField.IS_CHANGED, CustomField.REQUEST_PUBLISH)
                        .contains(key)) {
                    presentMetadata.removeCustomFieldValue(customFieldValue);
                    if (key.equals(CustomField.IS_PUBLISHING)) {
                        logger.info(String.format("Publishing of node %s finished", publicId));
                    }
                }
            });
        }
    }
}
