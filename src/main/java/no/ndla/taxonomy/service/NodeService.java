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
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.service.dtos.*;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.data.jpa.domain.Specification;
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
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final VersionService versionService;
    private final VersionRepository versionRepository;
    private final TreeSorter topicTreeSorter;
    private final NodeFetchcher nodeFetchcher;
    private final NodeSaver nodeSaver;

    public NodeService(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService, VersionRepository versionRepository,
            VersionService versionService, TreeSorter topicTreeSorter, NodeFetchcher nodeFetchcher,
            NodeSaver nodeSaver) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.versionRepository = versionRepository;
        this.versionService = versionService;
        this.topicTreeSorter = topicTreeSorter;
        this.nodeFetchcher = nodeFetchcher;
        this.nodeSaver = nodeSaver;
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

        return filtered.stream().map(n -> new NodeDTO(n, language.get())).collect(Collectors.toList());
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

    /**
     * Gets node including children, and copies from one schema to another
     * 
     * @param nodeId
     *            The node to copy
     * @param sourceId
     *            The version id of source schema. If null use default.
     * @param targetId
     *            The version id of target shenma. Fail if not present.
     */
    @Transactional
    public void publishNode(URI nodeId, Optional<URI> sourceId, URI targetId) {
        Version target = versionRepository.findFirstByPublicId(targetId)
                .orElseThrow(() -> new NotFoundServiceException("Target version not found! Aborting"));
        nodeFetchcher.setVersion(versionService.schemaFromHash(null)); // Defaults to current
        if (sourceId.isPresent()) {
            Version source = versionRepository.getByPublicId(sourceId.get());
            if (source != null) {
                // Use source to fetch object to publish
                // VersionContext.setCurrentVersion(versionService.schemaFromHash(source.getHash()));
                nodeFetchcher.setVersion(versionService.schemaFromHash(source.getHash()));
            }
        }
        Node node;
        try {
            nodeFetchcher.setPublicId(nodeId);
            ExecutorService es = Executors.newSingleThreadExecutor();
            Future<Node> future = es.submit(nodeFetchcher);
            node = future.get();
            es.shutdown();
        } catch (Exception e) {
            throw new NotFoundServiceException("Node was not found");
        }
        // Set target schema for updating
        try {
            // VersionContext.setCurrentVersion(versionService.schemaFromHash(target.getHash()));
            nodeSaver.setVersion(versionService.schemaFromHash(target.getHash()));
            nodeSaver.setType(node);
            ExecutorService es = Executors.newSingleThreadExecutor();
            Future<Node> future = es.submit(nodeSaver);
            node = future.get();
            es.shutdown();
        } catch (Exception e) {
            throw new NotFoundServiceException("Node was not found", e);
        }
        /*
         * for (Node child: children) { publishNode(child.getPublicId(), sourceId, targetId); }
         */
        /*
         * for (Resource resource: resources) { publishResource(resource); }
         */
    }
}
