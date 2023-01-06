/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.ChangelogRepository;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.dtos.*;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.task.Fetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import java.net.URI;
import java.util.HashSet;
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
public class NodeService implements SearchService<NodeDTO, Node, NodeRepository>, DisposableBean {
    Logger logger = LoggerFactory.getLogger(getClass().getName());
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final VersionService versionService;
    private final TreeSorter topicTreeSorter;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;
    private final RecursiveNodeTreeService recursiveNodeTreeService;
    private final TreeSorter treeSorter;

    private final ChangelogRepository changelogRepository;
    private final DomainEntityHelperService domainEntityHelperService;
    private final CustomFieldService customFieldService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
    }

    public NodeService(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService, VersionService versionService, TreeSorter topicTreeSorter,
            CachedUrlUpdaterService cachedUrlUpdaterService, ChangelogRepository changelogRepository,
            DomainEntityHelperService domainEntityHelperService, CustomFieldService customFieldService,
            RecursiveNodeTreeService recursiveNodeTreeService, TreeSorter treeSorter) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.versionService = versionService;
        this.topicTreeSorter = topicTreeSorter;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
        this.changelogRepository = changelogRepository;
        this.domainEntityHelperService = domainEntityHelperService;
        this.customFieldService = customFieldService;
        this.recursiveNodeTreeService = recursiveNodeTreeService;
        this.treeSorter = treeSorter;
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

    public NodeDTO getNode(URI publicId, String language) {
        var node = getNode(publicId);
        return new NodeDTO(node, language);
    }

    public Node getNode(URI publicId) {
        return nodeRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(publicId)
                .orElseThrow(() -> new NotFoundHttpResponseException("Topic was not found"));
    }

    public List<ResourceWithNodeConnectionDTO> getResourcesByNodeId(URI nodePublicId, Set<URI> resourceTypeIds,
            URI relevancePublicId, String languageCode, boolean recursive) {
        final var node = domainEntityHelperService.getNodeByPublicId(nodePublicId);

        final Set<Integer> topicIdsToSearchFor;

        // Add both topics and resourceTopics to a common list that will be sorted in a tree-structure based on rank at
        // each level
        final Set<ResourceTreeSortable<Node>> resourcesToSort = new HashSet<>();

        // Populate a list of topic IDs we are going to fetch first, and then fetch the actual topics later
        // This allows searching recursively without having to fetch the whole relation tree on each element in the
        // recursive logic. It is also necessary to have the tree information later for ordering the result
        if (recursive) {
            final var nodeList = recursiveNodeTreeService.getRecursiveNodes(node);

            nodeList.forEach(treeElement -> resourcesToSort.add(new ResourceTreeSortable<Node>("node", "node",
                    treeElement.getId(), treeElement.getParentId().orElse(0), treeElement.getRank())));

            topicIdsToSearchFor = nodeList.stream().map(RecursiveNodeTreeService.TreeElement::getId)
                    .collect(Collectors.toSet());
        } else {
            topicIdsToSearchFor = Set.of(node.getId());
        }

        return filterNodeResourcesByIdsAndReturn(topicIdsToSearchFor, resourceTypeIds, relevancePublicId,
                resourcesToSort, languageCode);
    }

    private List<ResourceWithNodeConnectionDTO> filterNodeResourcesByIdsAndReturn(Set<Integer> nodeIds,
            Set<URI> resourceTypeIds, URI relevance, Set<ResourceTreeSortable<Node>> sortableListToAddTo,
            String languageCode) {
        final List<NodeConnection> nodeResources;

        if (resourceTypeIds.size() > 0) {
            nodeResources = nodeConnectionRepository.getResourceBy(nodeIds, resourceTypeIds, relevance);
        } else {
            var nodeResourcesStream = nodeConnectionRepository.getByResourceIds(nodeIds).stream();
            if (relevance != null) {
                final var isRequestingCore = "urn:relevance:core".equals(relevance.toString());
                nodeResourcesStream = nodeResourcesStream.filter(nodeResource -> {
                    final var resource = nodeResource.getChild().orElse(null);
                    if (resource == null) {
                        return false;
                    }
                    final var rel = nodeResource.getRelevance().orElse(null);
                    if (rel != null) {
                        return rel.getPublicId().equals(relevance);
                    } else {
                        return isRequestingCore;
                    }
                });
            }
            nodeResources = nodeResourcesStream.collect(Collectors.toList());
        }

        nodeResources.forEach(nodeResource -> sortableListToAddTo.add(new ResourceTreeSortable<Node>(nodeResource)));

        // Sort the list, extract all the topicResource objects in between topics and return list of documents

        return treeSorter.sortList(sortableListToAddTo).stream().map(ResourceTreeSortable::getResourceConnection)
                .filter(Optional::isPresent).map(Optional::get)
                .map(wrappedNodeResource -> new ResourceWithNodeConnectionDTO((NodeConnection) wrappedNodeResource,
                        languageCode))
                .collect(Collectors.toList());

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
    public boolean makeAllResourcesPrimary(URI nodePublicId, boolean recursive) {
        final var node = nodeRepository.findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        if (recursive) {
            node.getChildren().forEach(nc -> nc.getChild().map(n -> makeAllResourcesPrimary(n.getPublicId(), true)));
        }

        node.getResourceChildren().forEach(cc -> cc.setPrimary(true));

        return node.getResourceChildren().stream()
                .allMatch(resourceConnection -> resourceConnection.isPrimary().orElse(false));
    }

    /**
     * Adds node and children to table to be processed later
     *
     * @param nodeId
     *            Public ID of the node to publish
     * @param sourceId
     *            Public ID of source schema. Default schema if not present
     * @param targetId
     *            Public ID of target schema. Mandatory
     * @param isRoot
     *            Used to save meta-field to track publishing
     * @param cleanUp
     *            Used to clean up metadata after publishing
     */
    @Async
    @Transactional
    public void publishNode(URI nodeId, Optional<URI> sourceId, URI targetId, boolean isRoot, boolean cleanUp) {
        String source = sourceId.flatMap(sid -> versionService.findVersionByPublicId(sid).map(Version::getHash))
                .orElse(null);
        String target = versionService.findVersionByPublicId(targetId).map(Version::getHash)
                .orElseThrow(() -> new NotFoundServiceException("Target version not found! Aborting"));

        Node node;
        try {
            Fetcher fetcher = new Fetcher();
            fetcher.setDomainEntityHelperService(domainEntityHelperService);
            fetcher.setCustomFieldService(customFieldService);
            fetcher.setVersion(versionService.schemaFromHash(source));
            fetcher.setPublicId(nodeId);
            fetcher.setAddIsPublishing(isRoot);
            Future<DomainEntity> future = executor.submit(fetcher);
            node = (Node) future.get();
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            throw new NotFoundServiceException("Failed to fetch node from source schema", e);
        }
        // At first run, makes sure node exists in db for node-connection to be saved.
        if (!cleanUp) {
            changelogRepository.save(new Changelog(source, target, nodeId, false));
        }
        for (NodeConnection connection : node.getChildren()) {
            if (connection.getChild().isPresent()) {
                Node child = connection.getChild().get();
                publishNode(child.getPublicId(), sourceId, targetId, false, cleanUp);
            }
            changelogRepository.save(new Changelog(source, target, connection.getPublicId(), cleanUp));
        }
        // When cleaning, node can be cleaned last to end with publish request to be stripped
        if (cleanUp) {
            changelogRepository.save(new Changelog(source, target, nodeId, true));
        } else {
            // Once more, with cleaning
            publishNode(nodeId, sourceId, targetId, false, true);
        }
        if (isRoot) {
            logger.info("Node " + nodeId + " added to changelog for publishing to " + target);
        }
    }

    public Node cloneNode(URI publicId, URI contentUri) {
        final var node = getNode(publicId);
        var cloned = new Node(node, false);
        cloned.setContentUri(contentUri);
        return nodeRepository.save(cloned);
    }
}
