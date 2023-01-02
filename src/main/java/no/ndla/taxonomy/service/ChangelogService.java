/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import liquibase.pro.packaged.R;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.ChangelogRepository;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.task.Fetcher;
import no.ndla.taxonomy.service.task.Updater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ChangelogService implements DisposableBean {
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ChangelogRepository changelogRepository;
    private final CustomFieldService customFieldService;
    private final DomainEntityHelperService domainEntityHelperService;
    private final VersionService versionService;
    private final ResourceTypeRepository resourceTypeRepository;
    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ChangelogService(ChangelogRepository changelogRepository, CustomFieldService customFieldService,
            DomainEntityHelperService domainEntityHelperService, VersionService versionService,
            ResourceTypeRepository resourceTypeRepository,
            ResourceResourceTypeRepository resourceResourceTypeRepository) {
        this.changelogRepository = changelogRepository;
        this.customFieldService = customFieldService;
        this.domainEntityHelperService = domainEntityHelperService;
        this.versionService = versionService;
        this.resourceTypeRepository = resourceTypeRepository;
        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
    }

    @Transactional
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void removeFinishedChangelogs() {
        changelogRepository.deleteByDoneTrue();
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void processChanges() {
        try {
            Optional<Changelog> maybeChangelog = changelogRepository.findFirstByDoneFalse();
            if (maybeChangelog.isPresent()) {
                Changelog changelog = maybeChangelog.get();
                if (changelog.isDone()) {
                    return;
                }
                DomainEntity entity;
                // Fetch
                try {
                    Fetcher fetcher = new Fetcher();
                    fetcher.setVersion(versionService.schemaFromHash(changelog.getSourceSchema()));
                    fetcher.setPublicId(changelog.getPublicId());
                    fetcher.setCleanUp(changelog.isCleanUp());
                    fetcher.setDomainEntityHelperService(domainEntityHelperService);
                    fetcher.setCustomFieldService(customFieldService); // For cleaning metadata

                    Future<DomainEntity> future = executor.submit(fetcher);
                    entity = future.get();
                } catch (Exception e) {
                    logger.info(e.getMessage(), e);
                    throw new NotFoundServiceException("Failed to fetch node from source schema", e);
                }
                // Update
                try {
                    Updater updater = new Updater();
                    updater.setVersion(versionService.schemaFromHash(changelog.getDestinationSchema()));
                    updater.setElement(entity);
                    updater.setCleanUp(changelog.isCleanUp());
                    updater.setChangelogService(this);

                    Future<DomainEntity> future = executor.submit(updater);
                    future.get();
                } catch (Exception e) {
                    logger.info(e.getMessage(), e);
                    throw new NotFoundServiceException("Failed to update entity", e);
                }
                changelog.setDone(true);
                changelogRepository.save(changelog);
            }
        } catch (Exception exception) {
            // Another server have already processed this element
        }

    }

    @Transactional
    public Optional<DomainEntity> updateNode(Node node, boolean cleanUp) {
        Node result;
        TaxonomyRepository<DomainEntity> repository = domainEntityHelperService.getRepository(node.getPublicId());
        Node existing = (Node) domainEntityHelperService.getEntityByPublicId(node.getPublicId());
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
                    domainEntityHelperService.deleteEntityByPublicId(nodeConnection.getPublicId());
                }
            });
            List<URI> resources = node.getNodeResources().stream().map(DomainEntity::getPublicId)
                    .collect(Collectors.toList());
            result.getNodeResources().forEach(nodeResource -> {
                if (!resources.contains(nodeResource.getPublicId())) {
                    domainEntityHelperService.deleteEntityByPublicId(nodeResource.getPublicId());
                }
            });
        }
        if (cleanUp) {
            domainEntityHelperService.buildPathsForEntity(result.getPublicId());
        }
        return Optional.of(result);
    }

    @Transactional
    public Optional<DomainEntity> updateResource(Resource resource, boolean cleanUp) {
        Resource result;
        TaxonomyRepository<DomainEntity> repository = domainEntityHelperService.getRepository(resource.getPublicId());
        Resource existing = (Resource) domainEntityHelperService.getEntityByPublicId(resource.getPublicId());
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
                toRemove.forEach(remove -> {
                    existing.removeResourceResourceType(remove);
                });
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
            domainEntityHelperService.buildPathsForEntity(result.getPublicId());
        }
        return Optional.of(result);
    }

    @Transactional
    public Optional<DomainEntity> updateNodeConnection(NodeConnection nodeConnection, boolean cleanUp) {
        NodeConnection result;
        TaxonomyRepository<DomainEntity> repository = domainEntityHelperService
                .getRepository(nodeConnection.getPublicId());
        TaxonomyRepository<DomainEntity> nodeRepository = domainEntityHelperService
                .getRepository(URI.create("urn:node:dummy"));

        NodeConnection existing = (NodeConnection) domainEntityHelperService
                .getEntityByPublicId(nodeConnection.getPublicId());
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
            domainEntityHelperService.buildPathsForEntity(result.getChild().get().getPublicId());
        }
        return Optional.of(result);
    }

    @Transactional
    public Optional<DomainEntity> updateNodeResource(NodeResource nodeResource, boolean cleanUp) {
        NodeResource result;
        TaxonomyRepository<DomainEntity> repository = domainEntityHelperService
                .getRepository(nodeResource.getPublicId());
        TaxonomyRepository<DomainEntity> nodeRepository = domainEntityHelperService
                .getRepository(URI.create("urn:node:dummy"));
        TaxonomyRepository<DomainEntity> resourceRepository = domainEntityHelperService
                .getRepository(URI.create("urn:resource:dummy"));

        NodeResource existing = (NodeResource) domainEntityHelperService
                .getEntityByPublicId(nodeResource.getPublicId());
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
            domainEntityHelperService.buildPathsForEntity(result.getResource().get().getPublicId());
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

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
    }
}
