/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
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
    ResourceRepository resourceRepository;

    @Autowired
    NodeResourceRepository nodeResourceRepository;

    protected Set<NodeConnection> children;
    protected Set<NodeResource> resources;

    public void setChildren(Set<NodeConnection> children) {
        this.children = children;
    }

    public void setResources(Set<NodeResource> resources) {
        this.resources = resources;
    }

    @Override
    protected Node callInternal() {
        Map<URI, Resource> resourceMap = updateAllResources();
        Node updatedNode = persistNode(this.type);
        // Connect children
        for (NodeConnection connection : children) {
            Optional<NodeConnection> connToUpdate = nodeConnectionRepository
                    .findFirstByPublicId(connection.getPublicId());
            if (connToUpdate.isPresent()) {
                connection.setId(connToUpdate.get().getId());
                nodeConnectionRepository.save(connToUpdate.get());
            } else {
                // Connect updated with child from connection
                Optional<Node> child = nodeRepository.findFirstByPublicId(connection.getChild().get().getPublicId());
                if (child.isPresent()) {
                    connection.setParent(updatedNode);
                    connection.setChild(child.get());
                    nodeConnectionRepository.save(new NodeConnection(connection));
                }
            }
        }
        // resources
        for (NodeResource nodeResource : resources) {
            Resource toSave = nodeResource.getResource().get();
            Optional<Resource> toUpdate = resourceRepository
                    .findFirstByPublicId(nodeResource.getResource().get().getPublicId());
            if (toUpdate.isPresent()) {
                toSave.setId(toUpdate.get().getId());
            } else {
                toSave.setId(null);
            }
            // Resource updatedResource = resourceRepository.save(toSave);
            // Connect node and resource
            Optional<NodeConnection> connToUpdate = nodeConnectionRepository
                    .findFirstByPublicId(nodeResource.getPublicId());
            if (connToUpdate.isPresent()) {
                nodeResource.setId(connToUpdate.get().getId());
                nodeResourceRepository.save(nodeResource);
            } else {
                nodeResourceRepository.save(NodeResource.create(updatedNode, resourceMap.get(toSave.getPublicId())));
            }
        }
        return updatedNode;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private Node persistNode(Node toSave) {
        ensureMetadataRefsExist(toSave.getMetadata());
        Optional<Node> toUpdate = nodeRepository.findFirstByPublicId(toSave.getPublicId());
        Node node = new Node(toSave);
        if (toUpdate.isPresent()) {
            node.setId(toUpdate.get().getId());
            toSave = nodeRepository.save(node);
        } else {
            toSave = nodeRepository.save(node);
        }
        return toSave;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        for (NodeResource nodeResource : resources) {
            Resource resource = nodeResource.getResource().get();
            ensureMetadataRefsExist(resource.getMetadata());
            Resource toSave = new Resource(resource);
            toSave.setMetadata(new Metadata(resource.getMetadata()));
            Optional<Resource> toUpdate = resourceRepository
                    .findFirstByPublicId(nodeResource.getResource().get().getPublicId());
            if (toUpdate.isPresent()) {
                toSave.setId(toUpdate.get().getId());
            }
            Resource updated = resourceRepository.save(toSave);
            resourceMap.put(updated.getPublicId(), updated);
        }
        return resourceMap;
    }
}
