/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.CustomFieldRepository;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.NodeResourceRepository;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.ResourceService;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;

@Component
public class NodeUpdater extends VersionSchemaUpdater<Node> {

    @Autowired
    CustomFieldRepository customFieldRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    @Lazy
    NodeService nodeService;

    @Autowired
    NodeConnectionRepository nodeConnectionRepository;

    @Autowired
    ResourceService resourceService;

    @Autowired
    NodeResourceRepository nodeResourceRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Optional<Node> callInternal() {
        Node toSave = this.element;
        Map<URI, Resource> resourceMap = updateAllResources();
        Node updated = persistNode(toSave);
        // Connect parent if present
        if (toSave.getParentNode().isPresent()) {
            Optional<Node> parent = nodeRepository.findFirstByPublicId(toSave.getParentNode().get().getPublicId());
            parent.ifPresent(p -> nodeConnectionRepository.save(NodeConnection.create(p, toSave)));
        }
        // Connect children
        for (NodeConnection connection : toSave.getChildren()) {
            Optional<NodeConnection> connToUpdate = nodeConnectionRepository
                    .findFirstByPublicId(connection.getPublicId());
            if (connToUpdate.isPresent()) {
                connection.setId(connToUpdate.get().getId());
                nodeConnectionRepository.save(connToUpdate.get());
            } else {
                // Connect updated with child from connection
                Optional<Node> child = nodeRepository
                        .fetchNodeGraphByPublicId(connection.getChild().get().getPublicId());
                if (child.isPresent()) {
                    Node unproxied = Hibernate.unproxy(child.get(), Node.class);
                    connection.setParent(updated);
                    connection.setChild(unproxied);
                    updated.addChildConnection(nodeConnectionRepository.save(new NodeConnection(connection)));
                }
            }
        }
        // Resources
        for (NodeResource nodeResource : toSave.getNodeResources()) {
            Resource resource = nodeResource.getResource().get();
            Resource existing = resourceMap.get(resource.getPublicId());
            // Connect node and resource
            Optional<NodeResource> connToUpdate = nodeResourceRepository
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
        nodeService.updatePaths(updated);
        return Optional.of(updated);
    }

    @Transactional
    private Node persistNode(Node toSave) {
        Node updated;
        ensureMetadataRefsExist(toSave.getMetadata(), customFieldRepository);
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
            mergeTranslations(present, toSave.getTranslations());
            updated = nodeRepository.save(present);
        }
        return updated;
    }

    private void mergeTranslations(Node present, Set<NodeTranslation> translations) {
        Set<NodeTranslation> updated = new HashSet<>();
        for (NodeTranslation translation : translations) {
            Optional<NodeTranslation> t = present.getTranslations().stream()
                    .filter(tr -> tr.getLanguageCode().equals(translation.getLanguageCode())).findFirst();
            if (t.isPresent()) {
                NodeTranslation tr = t.get();
                tr.setName(translation.getName());
                updated.add(tr);
            } else {
                updated.add(new NodeTranslation(translation, present));
            }
        }
        if (!updated.isEmpty()) {
            present.clearTranslations();
            for (NodeTranslation translation : updated) {
                present.addTranslation(translation);
            }
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
