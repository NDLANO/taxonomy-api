/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.ResourceService;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;

@Component
public class NodeUpdater extends VersionSchemaUpdater<Node> {
    Logger logger = LoggerFactory.getLogger(getClass().getName());

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

    @Autowired
    RelevanceRepository relevanceRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Optional<Node> callInternal() {
        Node toSave = this.element;
        Map<URI, Resource> resourceMap = updateAllResources();
        Node updated = persistNode(toSave);
        // Connect children
        for (NodeConnection connection : toSave.getChildren()) {
            Optional<NodeConnection> connToUpdate = nodeConnectionRepository
                    .findFirstByPublicId(connection.getPublicId());
            if (connToUpdate.isPresent()) {
                NodeConnection conn = connToUpdate.get();
                // update connection with data from other schema
                conn.setPrimary(connection.isPrimary().orElse(false));
                conn.setRank(connection.getRank());
                Relevance relevance = connection.getRelevance()
                        .map(rel -> relevanceRepository.getByPublicId(rel.getPublicId())).orElse(null);
                conn.setRelevance(relevance);
                nodeConnectionRepository.save(conn);
            } else {
                // Connect updated with child from connection
                Optional<Node> child = nodeRepository
                        .fetchNodeGraphByPublicId(connection.getChild().get().getPublicId());
                if (child.isPresent()) {
                    Node node = child.get();
                    if (node.getParentConnection().isPresent()) {
                        // Child already have parent, the node must have been moved!
                        URI publicId = node.getParentConnection().get().getPublicId();
                        node.releaseParentConnections();
                        node = nodeRepository.save(node);
                        nodeConnectionRepository.deleteByPublicId(publicId);
                    }
                    connection.setParent(updated);
                    connection.setChild(node);
                    Relevance relevance = connection.getRelevance()
                            .map(rel -> relevanceRepository.getByPublicId(rel.getPublicId())).orElse(null);
                    connection.setRelevance(relevance);
                    NodeConnection nodeConnection = nodeConnectionRepository.save(new NodeConnection(connection));
                    updated.addChildConnection(nodeConnection);
                }
            }
        }
        // Resources
        Set<NodeResource> nodeResources = new HashSet<>();
        for (NodeResource nodeResource : toSave.getNodeResources()) {
            Resource resource = nodeResource.getResource().get();
            Resource existing = resourceMap.get(resource.getPublicId());
            // Connect node and resource
            Optional<NodeResource> connToUpdate = nodeResourceRepository
                    .findFirstByPublicId(nodeResource.getPublicId());
            if (connToUpdate.isPresent()) {
                NodeResource res = connToUpdate.get();
                res.setNode(updated);
                res.setResource(existing);
                res.setPrimary(nodeResource.isPrimary().orElse(false));
                res.setRank(nodeResource.getRank());
                Relevance relevance = nodeResource.getRelevance()
                        .map(rel -> relevanceRepository.getByPublicId(rel.getPublicId())).orElse(null);
                res.setRelevance(relevance);
                nodeResources.add(persistNodeResource(res));
            } else {
                nodeResource.setNode(updated);
                nodeResource.setResource(existing);
                Relevance relevance = nodeResource.getRelevance()
                        .map(rel -> relevanceRepository.getByPublicId(rel.getPublicId())).orElse(null);
                nodeResource.setRelevance(relevance);
                try {
                    nodeResources.add(persistNodeResource(new NodeResource(nodeResource)));
                } catch (DataIntegrityViolationException e) {
                    logger.info("Connection already exists", e);
                    // connection exist with other name. Do nothing
                }
            }
        }
        updated.setNodeResources(nodeResources);
        return Optional.of(nodeService.updatePaths(updated));
    }

    @Transactional
    private NodeResource persistNodeResource(NodeResource nodeResource) {
        return nodeResourceRepository.saveAndFlush(nodeResource);
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
