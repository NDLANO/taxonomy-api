/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.ChangelogRepository;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi.LanguageFieldDTO;
import no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi.SearchableTaxonomyResourceType;
import no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi.TaxonomyContextDTO;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeConnectionDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.task.Fetcher;
import no.ndla.taxonomy.util.TitleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Transactional(readOnly = true)
@Service
public class NodeService implements SearchService<NodeDTO, Node, NodeRepository>, DisposableBean {
    Logger logger = LoggerFactory.getLogger(getClass().getName());
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeConnectionService connectionService;
    private final VersionService versionService;
    private final TreeSorter topicTreeSorter;
    private final ChangelogRepository changelogRepository;
    private final DomainEntityHelperService domainEntityHelperService;
    private final RecursiveNodeTreeService recursiveNodeTreeService;
    private final TreeSorter treeSorter;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;
    private final RelevanceRepository relevanceRepository;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
    }

    public NodeService(ChangelogRepository changelogRepository, DomainEntityHelperService domainEntityHelperService,
            NodeConnectionService connectionService, NodeConnectionRepository nodeConnectionRepository,
            NodeRepository nodeRepository, RecursiveNodeTreeService recursiveNodeTreeService,
            TreeSorter topicTreeSorter, TreeSorter treeSorter, VersionService versionService,
            CachedUrlUpdaterService cachedUrlUpdaterService, RelevanceRepository relevanceRepository) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.versionService = versionService;
        this.topicTreeSorter = topicTreeSorter;
        this.changelogRepository = changelogRepository;
        this.domainEntityHelperService = domainEntityHelperService;
        this.recursiveNodeTreeService = recursiveNodeTreeService;
        this.treeSorter = treeSorter;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
        this.relevanceRepository = relevanceRepository;
    }

    @Transactional
    public void delete(URI publicId) {
        final var nodeToDelete = nodeRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        connectionService.disconnectAllChildren(nodeToDelete);

        nodeRepository.delete(nodeToDelete);
        nodeRepository.flush();
    }

    public Specification<Node> nodeHasNodeType(NodeType nodeType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("nodeType"), nodeType);
    }

    public List<NodeDTO> getNodes(Optional<String> language, Optional<List<NodeType>> nodeType,
            Optional<URI> contentUri, Optional<String> contextId, Optional<Boolean> isRoot,
            MetadataFilters metadataFilters) {
        final List<Node> filtered = nodeRepository.findByNodeType(nodeType, metadataFilters.getVisible(),
                metadataFilters.getKey(), metadataFilters.getLikeQueryValue(), contentUri, contextId, isRoot);
        return filtered.stream().distinct().map(n -> new NodeDTO(Optional.empty(), n, language.get(), contextId))
                .collect(toList());
    }

    public List<NodeDTO> getNodesByType(Optional<List<NodeType>> nodeType, Optional<String> language,
            Optional<URI> contentUri, Optional<String> contextId, Optional<Boolean> isRoot,
            MetadataFilters metadataFilters) {
        final List<NodeDTO> listToReturn = new ArrayList<>();
        var ids = nodeRepository.findIdsByType(nodeType);
        final var counter = new AtomicInteger();
        ids.stream().collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000)).values().forEach(idChunk -> {
            final var nodes = nodeRepository.findByIdsFiltered(idChunk, metadataFilters.getVisible(),
                    metadataFilters.getKey(), metadataFilters.getLikeQueryValue(), contentUri, contextId, isRoot);
            var dtos = nodes.stream().map(node -> new NodeDTO(Optional.empty(), node, language.get(), contextId))
                    .toList();
            listToReturn.addAll(dtos);
        });

        return listToReturn;
    }

    public List<NodeConnectionDTO> getAllConnections(URI nodePublicId) {
        final var node = nodeRepository.findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        return Stream
                .concat(connectionService.getParentConnections(node).stream().map(NodeConnectionDTO::parentConnection),
                        connectionService.getChildConnections(node).stream().map(NodeConnectionDTO::childConnection))
                .toList();
    }

    public List<NodeChildDTO> getFilteredChildConnections(URI nodePublicId, String languageCode) {
        Node node = nodeRepository.findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        final List<NodeConnection> childConnections = nodeConnectionRepository
                .findAllByParentPublicIdIncludingChildAndChildTranslations(nodePublicId);

        final var wrappedList = childConnections.stream()
                .map(nodeConnection -> new NodeChildDTO(Optional.of(node), nodeConnection, languageCode)).toList();

        return topicTreeSorter.sortList(wrappedList);
    }

    public NodeDTO getNode(URI publicId, Optional<String> language) {
        var node = getNode(publicId);
        return new NodeDTO(Optional.empty(), node, language.orElse(Constants.DefaultLanguage), Optional.empty());
    }

    public Node getNode(URI publicId) {
        return nodeRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundHttpResponseException("Node was not found"));
    }

    public List<NodeChildDTO> getResourcesByNodeId(URI nodePublicId, Set<URI> resourceTypeIds, URI relevancePublicId,
            Optional<String> languageCode, boolean recursive) {
        final var node = domainEntityHelperService.getNodeByPublicId(nodePublicId);

        final Set<URI> topicIdsToSearchFor;

        // Add both topics and resourceTopics to a common list that will be sorted in a tree-structure based on rank at
        // each level
        final Set<ResourceTreeSortable> resourcesToSort = new HashSet<>();

        // Populate a list of topic IDs we are going to fetch first, and then fetch the actual topics later
        // This allows searching recursively without having to fetch the whole relation tree on each element in the
        // recursive logic. It is also necessary to have the tree information later for ordering the result
        if (recursive) {
            final var nodeList = recursiveNodeTreeService.getRecursiveNodes(node);

            nodeList.forEach(treeElement -> resourcesToSort.add(new ResourceTreeSortable("node", "node",
                    treeElement.getId(), treeElement.getParentId().orElse(URI.create("")), treeElement.getRank())));

            topicIdsToSearchFor = nodeList.stream().map(RecursiveNodeTreeService.TreeElement::getId)
                    .collect(Collectors.toSet());
        } else {
            topicIdsToSearchFor = Set.of(node.getPublicId());
        }

        return filterNodeResourcesByIdsAndReturn(node, topicIdsToSearchFor, resourceTypeIds, relevancePublicId,
                resourcesToSort, languageCode);
    }

    private List<NodeChildDTO> filterNodeResourcesByIdsAndReturn(Node root, Set<URI> nodeIds, Set<URI> resourceTypeIds,
            URI relevance, Set<ResourceTreeSortable> sortableListToAddTo, Optional<String> languageCode) {
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
            nodeResources = nodeResourcesStream.collect(toList());
        }

        nodeResources.forEach(nodeResource -> sortableListToAddTo.add(new ResourceTreeSortable(nodeResource)));

        // Sort the list, extract all the topicResource objects in between topics and return list of documents
        return treeSorter.sortList(sortableListToAddTo).stream().map(ResourceTreeSortable::getResourceConnection)
                .filter(src -> {
                    if (src.isEmpty())
                        return false;
                    var connection = (NodeConnection) src.get();
                    var childIsResource = connection.getChild().map(c -> c.getNodeType() == NodeType.RESOURCE);
                    return childIsResource.orElse(false);
                }).map(wrappedNodeResource -> {
                    NodeConnection nodeConnection = (NodeConnection) wrappedNodeResource.get();
                    return new NodeChildDTO(Optional.of(root), nodeConnection,
                            languageCode.orElse(Constants.DefaultLanguage));
                }).collect(toList());
    }

    @Override
    public NodeRepository getRepository() {
        return nodeRepository;
    }

    @Override
    public NodeDTO createDTO(Node node, String languageCode) {
        return new NodeDTO(Optional.empty(), node, languageCode, Optional.empty());
    }

    public SearchResultDTO<NodeDTO> searchByNodeType(Optional<String> query, Optional<List<String>> ids,
            Optional<String> language, int pageSize, int page, Optional<NodeType> nodeType) {
        Optional<ExtraSpecification<Node>> nodeSpecLambda = nodeType.map(nt -> (s -> s.and(nodeHasNodeType(nt))));
        return SearchService.super.search(query, ids, language, pageSize, page, nodeSpecLambda);
    }

    @Transactional
    public boolean makeAllResourcesPrimary(URI nodePublicId, boolean recursive) {
        final var node = nodeRepository.findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        if (recursive) {
            node.getChildConnections().forEach(nc -> {
                nc.getChild().filter(n -> n.getNodeType() != NodeType.RESOURCE)
                        .map(n -> makeAllResourcesPrimary(n.getPublicId(), true));
            });
        }

        node.getResourceChildren().forEach(cc -> {
            connectionService.updateParentChild(cc, cc.getRelevance().orElse(null), cc.getRank(), Optional.of(true));
        });

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
        for (NodeConnection connection : node.getChildConnections()) {
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

    public List<TaxonomyContextDTO> getSearchableByContentUri(Optional<URI> contentURI, boolean filterVisibles) {
        var nodes = nodeRepository.findByNodeType(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), contentURI, Optional.empty(), Optional.empty());
        var contextDtos = nodesToContexts(nodes, filterVisibles);

        return contextDtos.stream().sorted(Comparator.comparing(TaxonomyContextDTO::path)).toList();
    }

    List<TaxonomyContextDTO> nodesToContexts(List<Node> nodes, boolean filterVisibles) {
        return nodes.stream().flatMap(node -> {
            var contexts = filterVisibles
                    ? node.getContexts().stream().filter(Context::isVisible).collect(Collectors.toSet())
                    : node.getContexts();
            return contexts.stream().map(context -> {
                Optional<Relevance> relevance = relevanceRepository
                        .findFirstByPublicIdIncludingTranslations(URI.create(context.relevanceId()));
                var relevanceName = new LanguageField<String>();
                if (relevance.isPresent()) {
                    relevanceName = LanguageField.fromNode(relevance.get());
                }
                var resourceTypes = node.getResourceTypes().stream().map(SearchableTaxonomyResourceType::new).toList();
                var breadcrumbs = context.breadcrumbs();
                return new TaxonomyContextDTO(node.getPublicId(), URI.create(context.rootId()),
                        LanguageFieldDTO.fromLanguageField(context.rootName()), context.path(),
                        LanguageFieldDTO.fromLanguageFieldList(breadcrumbs), context.contextType(),
                        URI.create(context.relevanceId()), LanguageFieldDTO.fromLanguageField(relevanceName),
                        resourceTypes, context.parentIds().stream().map(URI::create).toList(), context.isPrimary(),
                        context.isActive(), context.isVisible(), context.contextId());
            });
        }).toList();
    }

    public List<Node> buildAllContexts() {
        List<Node> rootNodes = nodeRepository.findByNodeType(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Boolean.TRUE));
        rootNodes.forEach(cachedUrlUpdaterService::updateCachedUrls);
        return rootNodes;
    }

    public List<TaxonomyContextDTO> getContextByPath(Optional<String> path) {
        if (path.isPresent()) {
            String contextId = TitleUtil.getHashFromPath(path.get());
            List<Node> nodes = nodeRepository.findByNodeType(Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.of(contextId), Optional.empty());
            var contexts = nodesToContexts(nodes, false);
            return contexts.stream().filter(c -> c.contextId().equals(contextId)).toList();
        }
        return List.of();
    }
}
