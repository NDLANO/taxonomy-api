/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.dtos.*;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.task.NodeFetcher;
import no.ndla.taxonomy.service.task.NodeMetadataCleaner;
import no.ndla.taxonomy.service.task.NodeUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.jpa.domain.Specification.where;

@Transactional(readOnly = true)
@Service
public class NodeService implements SearchService<NodeDTO, Node, NodeRepository> {
    Logger logger = LoggerFactory.getLogger(getClass().getName());
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final VersionService versionService;
    private final ResourceService resourceService;
    private final TreeSorter topicTreeSorter;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;

    @Autowired
    private NodeFetcher nodeFetchcher;
    @Autowired
    private NodeUpdater nodeUpdater;
    @Autowired
    private NodeMetadataCleaner nodeMetadataCleaner;

    public NodeService(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService, VersionService versionService, ResourceService resourceService,
            TreeSorter topicTreeSorter, CachedUrlUpdaterService cachedUrlUpdaterService) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.versionService = versionService;
        this.resourceService = resourceService;
        this.topicTreeSorter = topicTreeSorter;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    @Transactional
    public void delete(URI publicId) {
        final var nodeToDelete = nodeRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        connectionService.disconnectAllChildren(nodeToDelete);

        nodeRepository.delete(nodeToDelete);
        nodeRepository.flush();
    }

    public Specification<Node> base() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("id"));
    }

    public Specification<Node> nodeIsRoot() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("root"), true);
    }

    public Specification<Node> nodeIsVisible(Boolean visible) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("metadata").get("visible"), visible);
    }

    public Specification<Node> nodeHasNodeType(NodeType nodeType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("nodeType"), nodeType);
    }

    public Specification<Node> nodeHasContentUri(URI contentUri) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("contentUri"), contentUri);
    }

    public Specification<Node> nodeHasCustomKey(String key) {
        return (root, query, criteriaBuilder) -> {
            Join<Node, Metadata> nodeMetadataJoin = root.join("metadata");
            Join<Metadata, CustomFieldValue> join = nodeMetadataJoin.join("customFieldValues");
            return criteriaBuilder.equal(join.get("customField").get("key"), key);
        };
    }

    public Specification<Node> nodeHasCustomValue(String value) {
        return (root, query, criteriaBuilder) -> {
            Join<Node, Metadata> nodeMetadataJoin = root.join("metadata");
            Join<Metadata, CustomFieldValue> join = nodeMetadataJoin.join("customFieldValues");
            return criteriaBuilder.equal(join.get("value"), value);
        };
    }

    public List<EntityWithPathDTO> getNodes(Optional<String> language, Optional<NodeType> nodeType,
            Optional<URI> contentUri, Optional<Boolean> isRoot, MetadataFilters metadataFilters) {

        final List<Node> filtered;
        Specification<Node> specification = where(base());
        if (isRoot.isPresent()) {
            specification = specification.and(nodeIsRoot());
        }
        if (contentUri.isPresent()) {
            specification = specification.and(nodeHasContentUri(contentUri.get()));
        }
        if (nodeType.isPresent()) {
            specification = specification.and(nodeHasNodeType(nodeType.get()));
        }
        if (metadataFilters.getVisible().isPresent()) {
            specification = specification.and(nodeIsVisible(metadataFilters.getVisible().get()));
        }
        if (metadataFilters.getKey().isPresent()) {
            specification = specification.and(nodeHasCustomKey(metadataFilters.getKey().get()));
        }
        if (metadataFilters.getValue().isPresent()) {
            specification = specification.and(nodeHasCustomValue(metadataFilters.getValue().get()));
        }
        filtered = nodeRepository.findAll(specification);

        return filtered.stream().distinct().map(n -> new NodeDTO(n, language.get())).collect(Collectors.toList());
    }

    public List<ConnectionIndexDTO> getAllConnections(URI nodePublicId) {
        final var node = nodeRepository.findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        return Stream
                .concat(connectionService.getParentConnections(node).stream().map(ConnectionIndexDTO::parentConnection),
                        connectionService.getChildConnections(node).stream()
                                .filter(entity -> entity instanceof NodeConnection)
                                .map(ConnectionIndexDTO::childConnection))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<TopicChildDTO> getFilteredChildConnections(URI nodePublicId, String languageCode) {
        final var node = nodeRepository.findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        final List<NodeConnection> childConnections = nodeConnectionRepository
                .findAllByParentPublicIdIncludingChildAndChildTranslations(nodePublicId);

        final var wrappedList = childConnections.stream()
                .map(nodeConnection -> new TopicChildDTO(node, nodeConnection, languageCode))
                .collect(Collectors.toUnmodifiableList());

        return topicTreeSorter.sortList(wrappedList);
    }

    @Override
    public NodeRepository getRepository() {
        return nodeRepository;
    }

    @Override
    public NodeDTO createDTO(Node node, String languageCode) {
        return new NodeDTO(node, languageCode);
    }

    public SearchResultDTO<NodeDTO> searchByNodeType(Optional<String> query, Optional<List<String>> ids,
            Optional<String> language, int pageSize, int page, Optional<NodeType> nodeType) {
        Optional<ExtraSpecification<Node>> nodeSpecLambda = nodeType.map(nt -> (s -> s.and(nodeHasNodeType(nt))));
        return SearchService.super.search(query, ids, language, pageSize, page, nodeSpecLambda);
    }

    @Transactional
    public Node updatePaths(Node node) {
        Node saved = nodeRepository.save(node);
        cachedUrlUpdaterService.updateCachedUrls(saved);
        return saved;
    }

    @Transactional
    public boolean makeAllResourcesPrimary(URI nodePublicId) {
        final var node = nodeRepository.findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        node.getNodeResources().forEach(
                nr -> connectionService.updateNodeResource(nr, nr.getRelevance().orElse(null), true, nr.getRank()));
        return node.getNodeResources().stream()
                .allMatch(resourceConnection -> resourceConnection.isPrimary().orElse(false));
    }

    /**
     * Gets node including children, and copies from one schema to another
     *
     * @param nodeId
     *            The node to copy
     * @param sourceId
     *            The version id of source schema. If null use default.
     * @param targetId
     *            The version id of target shenma. Fail if not present.
     * @param addCustomField
     *            Only update temp metadata if true
     */
    @Async
    @Transactional
    public Node publishNode(URI nodeId, Optional<URI> sourceId, URI targetId, boolean addCustomField) {
        Version target = versionService.findVersionByPublicId(targetId)
                .orElseThrow(() -> new NotFoundServiceException("Target version not found! Aborting"));
        nodeFetchcher.setVersion(versionService.schemaFromHash(null)); // Defaults to current
        nodeMetadataCleaner.setVersion(versionService.schemaFromHash(null));
        if (sourceId.isPresent()) {
            Optional<Version> source = versionService.findVersionByPublicId(sourceId.get());
            // Use source to fetch object
            source.ifPresent(version -> {
                nodeFetchcher.setVersion(versionService.schemaFromHash(version.getHash()));
                nodeMetadataCleaner.setVersion(versionService.schemaFromHash(version.getHash()));
            });
        }
        Node node;
        try {
            nodeFetchcher.setPublicId(nodeId);
            nodeFetchcher.setAddCustomField(addCustomField);
            ExecutorService es = Executors.newSingleThreadExecutor();
            Future<Node> future = es.submit(nodeFetchcher);
            node = future.get();
            es.shutdown();
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            throw new NotFoundServiceException("Failed to fetch node from source schema", e);
        }
        // Need to save children first to avoid saving missing nodes.
        for (NodeConnection connection : node.getChildren()) {
            if (connection.getChild().isPresent()) {
                Node child = connection.getChild().get();
                publishNode(child.getPublicId(), sourceId, targetId, false);
            }
        }
        // Set target schema for updating
        try {
            nodeUpdater.setSourceId(sourceId);
            nodeUpdater.setTargetId(targetId);
            nodeUpdater.setVersion(versionService.schemaFromHash(target.getHash()));
            nodeUpdater.setElement(node);
            ExecutorService es = Executors.newSingleThreadExecutor();
            Future<Node> future = es.submit(nodeUpdater);
            node = future.get();
            es.shutdown();
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            throw new NotFoundServiceException("Failed to update node in target schema", e);
        }
        // clean metadata after updating if root
        if (addCustomField) {
            try {
                nodeMetadataCleaner.setPublicId(nodeId);
                ExecutorService es = Executors.newSingleThreadExecutor();
                Future<Node> future = es.submit(nodeMetadataCleaner);
                future.get();
                es.shutdown();
            } catch (Exception e) {
                logger.info(e.getMessage(), e);
                throw new NotFoundServiceException("Failed to reset metadata for source node", e);
            }
            logger.info("Node " + nodeId + " published to target " + targetId.toString());
        }
        return node;
    }
}
