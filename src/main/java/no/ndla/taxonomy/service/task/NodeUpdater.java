/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.ResourceService;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class NodeUpdater extends VersionSchemaUpdater<Node> {

    @Autowired
    CustomFieldRepository customFieldRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    NodeConnectionRepository nodeConnectionRepository;

    @Autowired
    ResourceService resourceService;

    @Autowired
    NodeResourceRepository nodeResourceRepository;

    @Autowired
    CachedUrlUpdaterService cachedUrlUpdaterService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Optional<Node> callInternal() {
        Map<URI, Resource> resourceMap = updateAllResources();
        Node updated = persistNode(this.element);
        // Connect parent if present
        if (this.element.getParentNode().isPresent()) {
            Optional<Node> parent = nodeRepository
                    .findFirstByPublicId(this.element.getParentNode().get().getPublicId());
            parent.ifPresent(p -> nodeConnectionRepository.save(NodeConnection.create(p, this.element)));
        }
        for (NodeConnection connection : this.element.getChildren()) {
            Optional<NodeConnection> connToUpdate = nodeConnectionRepository
                    .findFirstByPublicId(connection.getPublicId());
            if (connToUpdate.isPresent()) {
                connection.setId(connToUpdate.get().getId());
                nodeConnectionRepository.save(connToUpdate.get());
            } else {
                // Connect updated with child from connection
                Optional<Node> child = nodeRepository.findFirstByPublicId(connection.getChild().get().getPublicId());
                if (child.isPresent()) {
                    connection.setParent(updated);
                    connection.setChild(child.get());
                    nodeConnectionRepository.save(new NodeConnection(connection));
                }
            }
        }

        // Connect children
        for (NodeConnection connection : this.element.getChildren()) {
            Optional<NodeConnection> connToUpdate = nodeConnectionRepository
                    .findFirstByPublicId(connection.getPublicId());
            if (connToUpdate.isPresent()) {
                connection.setId(connToUpdate.get().getId());
                nodeConnectionRepository.save(connToUpdate.get());
            } else {
                // Connect updated with child from connection
                Optional<Node> child = nodeRepository.findFirstByPublicId(connection.getChild().get().getPublicId());
                if (child.isPresent()) {
                    connection.setParent(updated);
                    connection.setChild(child.get());
                    nodeConnectionRepository.save(new NodeConnection(connection));
                }
            }
        }
        // resources
        for (NodeResource nodeResource : this.element.getNodeResources()) {
            Resource resource = nodeResource.getResource().get();
            Resource existing = resourceMap.get(resource.getPublicId());
            // Resource updatedResource = resourceRepository.save(toSave);
            // Connect node and resource
            Optional<NodeConnection> connToUpdate = nodeConnectionRepository
                    .findFirstByPublicId(nodeResource.getPublicId());
            if (connToUpdate.isPresent()) {
                nodeResource.setId(connToUpdate.get().getId());
                nodeResourceRepository.save(nodeResource);
            } else {
                nodeResource.setNode(updated);
                nodeResource.setResource(existing);
                nodeResourceRepository.save(new NodeResource(nodeResource));
            }
        }
        return Optional.of(updated);
    }

    private Node persistNode(Node toSave) {
        Node updated;
        ensureMetadataRefsExist(toSave.getMetadata());
        Optional<Node> existing = nodeRepository.fetchNodeGraphByPublicId(toSave.getPublicId());
        if (!existing.isPresent()) {
            // Node is new
            Node node = new Node(toSave);
            updated = nodeRepository.save(node);
        } else {
            // Node exists, update current
            Node present = existing.get();
            present.setName(toSave.getName());
            present.setContentUri(toSave.getContentUri());
            present.setNodeType(toSave.getNodeType());
            mergeMetadata(present, toSave.getMetadata());
            updated = nodeRepository.save(present);
        }
        cachedUrlUpdaterService.updateCachedUrls(updated);
        return updated;
    }

    private void ensureMetadataRefsExist(Metadata metadata) {
        if (!metadata.getCustomFieldValues().isEmpty()) {
            metadata.getCustomFieldValues().forEach(customFieldValue -> {
                CustomField toSave = customFieldValue.getCustomField();
                CustomField saved;
                Optional<CustomField> byKey = customFieldRepository.findByKey(toSave.getKey());
                if (byKey.isPresent()) {
                    // Same customField, do nothing
                    saved = byKey.get();
                } else {
                    toSave.setId(null);
                    saved = customFieldRepository.save(toSave);
                }
                customFieldValue.setCustomField(saved);
            });
        }
    }

    @Transactional
    private Map<URI, Resource> updateAllResources() {
        Map<URI, Resource> resourceMap = new HashMap();
        for (NodeResource nodeResource : this.element.getNodeResources()) {
            URI publicId = nodeResource.getResource().get().getPublicId();
            Resource published = resourceService.publishResource(publicId, sourceId, targetId);
            resourceMap.put(publicId, published);
        }
        return resourceMap;
    }
}
