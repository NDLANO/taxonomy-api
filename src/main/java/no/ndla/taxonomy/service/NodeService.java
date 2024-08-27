/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.persistence.EntityManager;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.NodePostPut;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.LanguageFieldDTO;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.SearchableTaxonomyResourceType;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.TaxonomyContextDTO;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.TaxonomyCrumbDTO;
import no.ndla.taxonomy.service.dtos.*;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.util.PrettyUrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class NodeService implements SearchService<NodeDTO, Node, NodeRepository> {
    Logger logger = LoggerFactory.getLogger(getClass().getName());
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeConnectionService connectionService;
    private final TreeSorter topicTreeSorter;
    private final DomainEntityHelperService domainEntityHelperService;
    private final RecursiveNodeTreeService recursiveNodeTreeService;
    private final TreeSorter treeSorter;
    private final ContextUpdaterService cachedUrlUpdaterService;
    private final EntityManager entityManager;

    @Value(value = "${new.url.separator:false}")
    private boolean newUrlSeparator;

    public NodeService(
            DomainEntityHelperService domainEntityHelperService,
            NodeConnectionService connectionService,
            NodeConnectionRepository nodeConnectionRepository,
            NodeRepository nodeRepository,
            RecursiveNodeTreeService recursiveNodeTreeService,
            TreeSorter topicTreeSorter,
            TreeSorter treeSorter,
            ContextUpdaterService cachedUrlUpdaterService,
            EntityManager entityManager) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.topicTreeSorter = topicTreeSorter;
        this.domainEntityHelperService = domainEntityHelperService;
        this.recursiveNodeTreeService = recursiveNodeTreeService;
        this.treeSorter = treeSorter;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
        this.entityManager = entityManager;
    }

    @Transactional
    public void delete(URI publicId) {
        final var nodeToDelete = nodeRepository
                .findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        connectionService.disconnectAllChildren(nodeToDelete);

        nodeRepository.delete(nodeToDelete);
        nodeRepository.flush();
    }

    public Specification<Node> nodeHasOneOfNodeType(List<NodeType> nodeType) {
        return (root, query, builder) -> root.get("nodeType").in(nodeType);
    }

    public List<NodeDTO> getNodesByType(
            Optional<List<NodeType>> nodeType,
            String language,
            Optional<List<URI>> publicIds,
            Optional<URI> contentUri,
            Optional<String> contextId,
            Optional<Boolean> isRoot,
            Optional<Boolean> isContext,
            MetadataFilters metadataFilters,
            Optional<Boolean> includeContexts,
            boolean filterProgrammes,
            Optional<URI> rootId,
            Optional<URI> parentId) {
        final List<NodeDTO> listToReturn = new ArrayList<>();
        List<Integer> ids;
        if (contextId.isPresent()) {
            ids = nodeRepository.findIdsByContextId(contextId);
        } else {
            ids = nodeRepository.findIdsFiltered(
                    nodeType,
                    publicIds,
                    metadataFilters.getVisible(),
                    metadataFilters.getKey(),
                    metadataFilters.getLikeQueryValue(),
                    contentUri,
                    isRoot,
                    isContext);
        }
        final var counter = new AtomicInteger();
        var root = rootId.map(this::getNode);
        var parent = parentId.map(this::getNode);
        ids.stream()
                .collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000))
                .values()
                .forEach(idChunk -> {
                    final var nodes = nodeRepository.findByIds(idChunk);
                    var dtos = nodes.stream()
                            .map(node -> new NodeDTO(
                                    root,
                                    parent,
                                    node,
                                    language,
                                    contextId,
                                    includeContexts,
                                    filterProgrammes,
                                    newUrlSeparator))
                            .toList();
                    listToReturn.addAll(dtos);
                });

        return listToReturn;
    }

    public List<ConnectionDTO> getAllConnections(URI nodePublicId) {
        final var node = nodeRepository
                .findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        return Stream.concat(
                        connectionService.getParentConnections(node).stream().map(ConnectionDTO::parentConnection),
                        connectionService.getChildConnections(node).stream().map(ConnectionDTO::childConnection))
                .toList();
    }

    public List<NodeChildDTO> getFilteredChildConnections(URI nodePublicId, String languageCode) {
        Node node = nodeRepository
                .findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        final List<NodeConnection> childConnections =
                nodeConnectionRepository.findAllByParentPublicIdIncludingChildAndChildTranslations(nodePublicId);

        final var wrappedList = childConnections.stream()
                .map(nodeConnection -> new NodeChildDTO(
                        Optional.of(node), nodeConnection, languageCode, Optional.of(false), false, newUrlSeparator))
                .toList();

        return topicTreeSorter.sortList(wrappedList);
    }

    public NodeDTO getNode(
            URI publicId,
            Optional<String> language,
            Optional<URI> rootId,
            Optional<URI> parentId,
            Optional<Boolean> includeContexts,
            boolean filterProgrammes) {
        var node = getNode(publicId);
        var root = rootId.map(this::getNode);
        var parent = parentId.map(this::getNode);
        return new NodeDTO(
                root,
                parent,
                node,
                language.orElse(Constants.DefaultLanguage),
                Optional.empty(),
                includeContexts,
                filterProgrammes,
                newUrlSeparator);
    }

    public Optional<Node> getMaybeNode(URI publicId) {
        return nodeRepository.findFirstByPublicId(publicId);
    }

    public Node getNode(URI publicId) {
        return getMaybeNode(publicId).orElseThrow(() -> new NotFoundHttpResponseException("Node was not found"));
    }

    public List<NodeChildDTO> getResourcesByNodeId(
            URI nodePublicId,
            Optional<List<URI>> resourceTypeIds,
            Optional<URI> relevanceId,
            Optional<String> languageCode,
            boolean recursive,
            Optional<Boolean> includeContexts,
            boolean filterProgrammes) {
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

            nodeList.forEach(treeElement -> resourcesToSort.add(new ResourceTreeSortable(
                    "node",
                    "node",
                    treeElement.getId(),
                    treeElement.getParentId().orElse(URI.create("")),
                    treeElement.getRank())));

            topicIdsToSearchFor = nodeList.stream()
                    .map(RecursiveNodeTreeService.TreeElement::getId)
                    .collect(Collectors.toSet());
        } else {
            topicIdsToSearchFor = Set.of(node.getPublicId());
        }

        return filterNodeResourcesByIdsAndReturn(
                node,
                topicIdsToSearchFor,
                resourceTypeIds,
                relevanceId,
                resourcesToSort,
                languageCode,
                includeContexts,
                filterProgrammes);
    }

    private List<NodeChildDTO> filterNodeResourcesByIdsAndReturn(
            Node root,
            Set<URI> nodeIds,
            Optional<List<URI>> resourceTypeIds,
            Optional<URI> relevanceId,
            Set<ResourceTreeSortable> sortableListToAddTo,
            Optional<String> languageCode,
            Optional<Boolean> includeContexts,
            boolean filterProgrammes) {
        final List<NodeConnection> nodeResources;

        var relevanceEnum = relevanceId.flatMap(Relevance::getRelevance);
        var nodeResourcesStream =
                nodeConnectionRepository.getResourceBy(nodeIds, resourceTypeIds, relevanceEnum).stream();
        if (relevanceId.isPresent()) {
            final var isRequestingCore = "urn:relevance:core".equals(relevanceId.toString());
            nodeResourcesStream = nodeResourcesStream.filter(nodeResource -> {
                final var resource = nodeResource.getChild().orElse(null);
                if (resource == null) {
                    return false;
                }
                final var rel = nodeResource.getRelevance().orElse(null);
                if (rel != null) {
                    return rel.getPublicId().equals(relevanceId.get());
                } else {
                    return isRequestingCore;
                }
            });
        }
        nodeResources = nodeResourcesStream.toList();

        nodeResources.forEach(nodeResource -> sortableListToAddTo.add(new ResourceTreeSortable(nodeResource)));

        // Sort the list, extract all the topicResource objects in between topics and return list of documents
        return treeSorter.sortList(sortableListToAddTo).stream()
                .map(ResourceTreeSortable::getResourceConnection)
                .filter(src -> {
                    if (src.isEmpty()) return false;
                    var connection = (NodeConnection) src.get();
                    var childIsResource = connection.getChild().map(c -> c.getNodeType() == NodeType.RESOURCE);
                    return childIsResource.orElse(false);
                })
                .map(wrappedNodeResource -> {
                    NodeConnection nodeConnection = (NodeConnection) wrappedNodeResource.get();
                    return new NodeChildDTO(
                            Optional.of(root),
                            nodeConnection,
                            languageCode.orElse(Constants.DefaultLanguage),
                            includeContexts,
                            filterProgrammes,
                            newUrlSeparator);
                })
                .toList();
    }

    @Override
    public NodeRepository getRepository() {
        return nodeRepository;
    }

    @Override
    public NodeDTO createDTO(
            Node node, String languageCode, Optional<Boolean> includeContexts, boolean filterProgrammes) {
        return new NodeDTO(
                Optional.empty(),
                Optional.empty(),
                node,
                languageCode,
                Optional.empty(),
                includeContexts,
                filterProgrammes,
                newUrlSeparator);
    }

    public SearchResultDTO<NodeDTO> searchByNodeType(
            Optional<String> query,
            Optional<List<String>> ids,
            Optional<List<String>> contentUris,
            Optional<String> language,
            Optional<Boolean> includeContexts,
            boolean filterProgrammes,
            int pageSize,
            int page,
            Optional<List<NodeType>> nodeType,
            Optional<Map<String, String>> customfieldsFilter) {
        Optional<ExtraSpecification<Node>> nodeSpecLambda = nodeType.map(nt -> (s -> s.and(nodeHasOneOfNodeType(nt))));
        return SearchService.super.search(
                query,
                ids,
                contentUris,
                language,
                includeContexts,
                filterProgrammes,
                pageSize,
                page,
                nodeSpecLambda,
                customfieldsFilter);
    }

    @Transactional
    public boolean makeAllResourcesPrimary(URI nodePublicId, boolean recursive) {
        final var node = nodeRepository
                .findFirstByPublicId(nodePublicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        if (recursive) {
            node.getChildConnections().forEach(nc -> {
                nc.getChild()
                        .filter(n -> n.getNodeType() != NodeType.RESOURCE)
                        .map(n -> makeAllResourcesPrimary(n.getPublicId(), true));
            });
        }

        node.getResourceChildren().forEach(cc -> {
            connectionService.updateParentChild(
                    cc, cc.getRelevance().orElse(null), Optional.of(cc.getRank()), Optional.of(true));
        });

        return node.getResourceChildren().stream()
                .allMatch(resourceConnection -> resourceConnection.isPrimary().orElse(false));
    }

    public Node cloneNode(URI publicId, Optional<URI> contentUri) {
        final var node = getNode(publicId);
        var cloned = new Node(node, false);
        cloned.setContentUri(contentUri.orElse(null)); // Set to null if not provided
        return nodeRepository.save(cloned);
    }

    public List<TaxonomyContextDTO> getSearchableByContentUri(
            Optional<URI> contentURI, boolean filterVisibles, String language) {
        var nodes = nodeRepository.findByContentUri(contentURI);
        var contextDtos = nodesToContexts(nodes, filterVisibles, language);

        return contextDtos.stream()
                .sorted(Comparator.comparing(TaxonomyContextDTO::path))
                .toList();
    }

    public List<TaxonomyContextDTO> nodesToContexts(List<Node> nodes, boolean filterVisibles, String language) {
        return nodes.stream()
                .flatMap(node -> {
                    var contexts = filterVisibles
                            ? node.getContexts().stream()
                                    .filter(TaxonomyContext::isVisible)
                                    .collect(Collectors.toSet())
                            : node.getContexts();
                    return contexts.stream().map(context -> {
                        Optional<Relevance> relevance = Relevance.getRelevance(URI.create(context.relevanceId()));
                        var relevanceName = new LanguageField<String>();
                        if (relevance.isPresent()) {
                            relevanceName = LanguageField.fromRelevance(relevance.get());
                        }
                        var resourceTypes = node.getResourceTypes().stream()
                                .map(SearchableTaxonomyResourceType::new)
                                .toList();
                        var breadcrumbs = context.breadcrumbs();
                        var parents = context.parents().stream()
                                .map(parent -> {
                                    var url = PrettyUrlUtil.createPrettyUrl(
                                            Optional.ofNullable(context.rootName()),
                                            parent.name(),
                                            language,
                                            parent.contextId(),
                                            parent.nodeType(),
                                            newUrlSeparator);
                                    return new TaxonomyCrumbDTO(
                                            URI.create(parent.id()),
                                            parent.contextId(),
                                            LanguageFieldDTO.fromLanguageField(parent.name()),
                                            parent.path(),
                                            url.orElse(parent.path()));
                                })
                                .toList();
                        return new TaxonomyContextDTO(
                                node.getPublicId(),
                                node.getPublicId(),
                                URI.create(context.rootId()),
                                LanguageFieldDTO.fromLanguageField(context.rootName()),
                                context.path(),
                                LanguageFieldDTO.fromLanguageFieldList(breadcrumbs),
                                context.contextType(),
                                URI.create(context.relevanceId()),
                                LanguageFieldDTO.fromLanguageField(relevanceName),
                                resourceTypes,
                                context.parentIds().stream().map(URI::create).toList(),
                                context.parentContextIds(),
                                context.isPrimary(),
                                context.isActive(),
                                context.isVisible(),
                                context.contextId(),
                                context.rank(),
                                context.connectionId(),
                                PrettyUrlUtil.createPrettyUrl(
                                        Optional.of(context.rootName()),
                                        LanguageField.fromNode(node),
                                        language,
                                        context.contextId(),
                                        node.getNodeType(),
                                        newUrlSeparator),
                                parents);
                    });
                })
                .toList();
    }

    @Async
    @Transactional
    public void buildAllContextsAsync() {
        buildAllContexts();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    protected List<Node> buildAllContexts() {
        logger.info("Building contexts for all roots in schema");
        var startTime = System.currentTimeMillis();
        List<Node> rootNodes = nodeRepository.findSubjectRoots();
        rootNodes.forEach(this::buildContexts);
        logger.info("Building contexts for all roots. took {} ms", System.currentTimeMillis() - startTime);
        return rootNodes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void buildContexts(Node entity) {
        logger.debug("Building contexts for node {}", entity.getName());
        cachedUrlUpdaterService.updateContexts(entity);
        entityManager.flush();
    }

    @Transactional
    public void updateQualityEvaluationOfParents(
            URI nodeId, NodeType nodeType, Optional<Grade> oldGrade, UpdatableDto<?> command) {
        if (nodeType != NodeType.RESOURCE) {
            return;
        }

        if (!(command instanceof NodePostPut nodeCommand)) {
            return;
        }

        Optional<QualityEvaluationDTO> qe =
                nodeCommand.qualityEvaluation.isDelete() ? Optional.empty() : nodeCommand.qualityEvaluation.getValue();
        var newGrade = qe.map(QualityEvaluationDTO::getGrade);
        if (oldGrade.isEmpty() && newGrade.isEmpty()) {
            return;
        }

        var node = nodeRepository
                .findFirstByPublicId(nodeId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        updateQualityEvaluationOf(node.getParentNodes(), oldGrade, newGrade);
    }

    @Transactional
    public void updateQualityEvaluationOf(
            Collection<Node> parents, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        var parentIds = parents.stream().map(DomainEntity::getPublicId).toList();
        updateQualityEvaluationOfRecursive(parentIds, oldGrade, newGrade);
    }

    @Transactional
    protected void updateQualityEvaluationOfRecursive(
            List<URI> parentIds, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        parentIds.forEach(pid -> nodeRepository.findFirstByPublicId(pid).ifPresent(p -> {
            p.updateChildQualityEvaluationAverage(oldGrade, newGrade);
            nodeRepository.save(p);
            var parentsParents =
                    p.getParentNodes().stream().map(Node::getPublicId).toList();
            updateQualityEvaluationOfRecursive(parentsParents, oldGrade, newGrade);
        }));
    }

    public List<TaxonomyContextDTO> getContextByPath(Optional<String> path, String language) {
        return path.map(p -> getContextByContextId(Optional.of(PrettyUrlUtil.getHashFromPath(p)), language))
                .orElse(List.of());
    }

    public List<TaxonomyContextDTO> getContextByContextId(Optional<String> contextId, String language) {
        if (contextId.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = nodeRepository.findIdsByContextId(contextId);
        var nodes = nodeRepository.findByIds(ids);
        var contexts = nodesToContexts(nodes, false, language);
        return contexts.stream()
                .filter(c -> c.contextId().equals(contextId.get()))
                .toList();
    }
}
