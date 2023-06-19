/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.commands.NodePostPut;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/nodes"})
public class Nodes extends CrudControllerWithMetadata<Node> {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeService nodeService;
    private final RecursiveNodeTreeService recursiveNodeTreeService;
    private final TreeSorter treeSorter;

    public Nodes(
            NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository,
            NodeService nodeService,
            ContextUpdaterService cachedUrlUpdaterService,
            RecursiveNodeTreeService recursiveNodeTreeService,
            TreeSorter treeSorter) {
        super(nodeRepository, cachedUrlUpdaterService);

        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeService = nodeService;
        this.recursiveNodeTreeService = recursiveNodeTreeService;
        this.treeSorter = treeSorter;
    }

    private List<NodeType> getDefaultNodeTypes(
            Optional<List<NodeType>> nodeType,
            Optional<URI> contentURI,
            Optional<String> contextId,
            Optional<Boolean> isContext,
            MetadataFilters metadataFilters) {
        if (nodeType.isPresent() && nodeType.get().size() > 0) {
            return nodeType.get();
        }
        if (contentURI.isEmpty() && contextId.isEmpty() && isContext.isEmpty() && !metadataFilters.hasFilters()) {
            return List.of(NodeType.TOPIC, NodeType.NODE, NodeType.SUBJECT, NodeType.PROGRAMME);
        }
        return List.of(NodeType.values());
    }

    @GetMapping
    @Operation(summary = "Gets all nodes")
    @Transactional(readOnly = true)
    public List<NodeDTO> getAllNodes(
            @Parameter(
                            description =
                                    "Filter by nodeType, could be a comma separated list, defaults to Topics and Subjects (Resources are quite slow). :^)")
                    @RequestParam(value = "nodeType", required = false)
                    Optional<List<NodeType>> nodeType,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    Optional<String> language,
            @Parameter(description = "Filter by contentUri") @RequestParam(value = "contentURI", required = false)
                    Optional<URI> contentUri,
            @Parameter(description = "Only root level contexts", deprecated = true)
                    @RequestParam(value = "isRoot", required = false)
                    Optional<Boolean> isRoot,
            @Parameter(description = "Only contexts") @RequestParam(value = "isContext", required = false)
                    Optional<Boolean> isContext,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "key", required = false)
                    Optional<String> key,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "value", required = false)
                    Optional<String> value,
            @Parameter(description = "Filter by visible") @RequestParam(value = "isVisible", required = false)
                    Optional<Boolean> isVisible,
            @Parameter(description = "Filter by context id") @RequestParam(value = "contextId", required = false)
                    Optional<String> contextId,
            @Parameter(description = "Include all contexts") @RequestParam(value = "includeContexts", required = false)
                    Optional<Boolean> includeContexts) {
        MetadataFilters metadataFilters = new MetadataFilters(key, value, isVisible);
        var isRootOrContext = isRoot.isPresent() ? isRoot : isContext;
        var defaultNodeTypes = getDefaultNodeTypes(nodeType, contentUri, contextId, isRootOrContext, metadataFilters);
        return nodeService.getNodesByType(
                Optional.of(defaultNodeTypes),
                language,
                contentUri,
                contextId,
                isRoot,
                isContext,
                metadataFilters,
                includeContexts);
    }

    @GetMapping("/search")
    @Operation(summary = "Search all nodes")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> searchNodes(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    Optional<String> language,
            @Parameter(description = "How many results to return per page")
                    @RequestParam(value = "pageSize", defaultValue = "10")
                    int pageSize,
            @Parameter(description = "Which page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Query to search names") @RequestParam(value = "query", required = false)
                    Optional<String> query,
            @Parameter(description = "Ids to fetch for query") @RequestParam(value = "ids", required = false)
                    Optional<List<String>> ids,
            @Parameter(description = "ContentURIs to fetch for query")
                    @RequestParam(value = "contentUris", required = false)
                    Optional<List<String>> contentUris,
            @Parameter(description = "Filter by nodeType") @RequestParam(value = "nodeType", required = false)
                    Optional<NodeType> nodeType,
            @Parameter(description = "Include all contexts") @RequestParam(value = "includeContexts", required = false)
                    Optional<Boolean> includeContexts) {

        return nodeService.searchByNodeType(
                query, ids, contentUris, language, includeContexts, pageSize, page, nodeType);
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all nodes paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> getNodePage(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    Optional<String> language,
            @Parameter(name = "page", description = "The page to fetch") Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch") Optional<Integer> pageSize,
            @Parameter(name = "includeContexts", description = "Include all contexts")
                    Optional<Boolean> includeContexts) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1) throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeRepository.findIdsPaginated(PageRequest.of(page.get() - 1, pageSize.get()));
        var results = nodeRepository.findByIds(ids.getContent());
        var contents = results.stream()
                .map(node ->
                        new NodeDTO(Optional.empty(), node, language.orElse("nb"), Optional.empty(), includeContexts))
                .collect(Collectors.toList());
        return new SearchResultDTO<>(ids.getTotalElements(), page.get(), pageSize.get(), contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single node")
    @Transactional(readOnly = true)
    public NodeDTO getNode(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    Optional<String> language) {
        return nodeService.getNode(id, language, Optional.of(true));
    }

    @PostMapping
    @Operation(
            summary = "Creates a new node",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createNode(
            @Parameter(name = "connection", description = "The new node") @RequestBody @Schema(name = "NodePOST")
                    NodePostPut command) {
        return createEntity(new Node(command.nodeType), command);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Updates a single node",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateNode(
            @PathVariable("id") URI id,
            @Parameter(name = "node", description = "The updated node. Fields not included will be set to null.")
                    @RequestBody
                    @Schema(name = "NodePUT")
                    NodePostPut command) {
        updateEntity(id, command);
    }

    @PutMapping("/{id}/publish")
    @Operation(
            summary = "Publishes a node hierarchy to a version",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    public void publishNode(
            @PathVariable("id") URI id,
            @Parameter(
                            description = "Version id to publish from. Can be omitted to publish from default.",
                            example = "urn:version:1")
                    @RequestParam(value = "sourceId", required = false)
                    Optional<URI> sourceId,
            @Parameter(description = "Version id to publish to.", example = "urn:version:2")
                    @RequestParam(value = "targetId")
                    URI targetId) {
        nodeService.publishNode(id, sourceId, targetId, true, false);
    }

    @GetMapping("/{id}/nodes")
    @Operation(summary = "Gets all children for this node")
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getChildren(
            @Parameter(name = "id", required = true) @PathVariable("id") URI id,
            @Parameter(
                            description =
                                    "Filter by nodeType, could be a comma separated list, defaults to Topics and Subjects (Resources are quite slow). :^)")
                    @RequestParam(value = "nodeType", required = false)
                    Optional<List<NodeType>> nodeType,
            @Parameter(description = "If true, children are fetched recursively")
                    @RequestParam(value = "recursive", required = false, defaultValue = "false")
                    boolean recursive,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    Optional<String> language,
            @Parameter(description = "Include all contexts") @RequestParam(value = "includeContexts", required = false)
                    Optional<Boolean> includeContexts) {
        final var node = nodeRepository.findFirstByPublicId(id).orElseThrow(() -> new NotFoundException("Node", id));

        final List<URI> childrenIds;
        final List<NodeType> nodeTypes = nodeType.orElse(List.of(NodeType.NODE, NodeType.TOPIC, NodeType.SUBJECT));
        if (recursive) {
            childrenIds = recursiveNodeTreeService.getRecursiveNodes(node, nodeTypes).stream()
                    .map(RecursiveNodeTreeService.TreeElement::getId)
                    .collect(Collectors.toList());
        } else {
            childrenIds = node.getChildConnections().stream()
                    .map(NodeConnection::getChild)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(n -> nodeTypes.contains(n.getNodeType()))
                    .map(Node::getPublicId)
                    .collect(Collectors.toList());
        }
        final var children =
                nodeConnectionRepository.findAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(childrenIds);

        final var returnList = new ArrayList<NodeChildDTO>();

        children.stream()
                .map(nodeConnection -> new NodeChildDTO(
                        Optional.of(node), nodeConnection, language.orElse(Constants.DefaultLanguage), includeContexts))
                .forEach(returnList::add);

        var filtered = returnList.stream()
                .filter(childDTO -> childrenIds.contains(childDTO.getParentId())
                        || node.getPublicId().equals(childDTO.getParentId()))
                .toList();

        return treeSorter.sortList(filtered).stream().distinct().collect(Collectors.toList());
    }

    @GetMapping("/{id}/connections")
    @Operation(summary = "Gets all parents and children this node is connected to")
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getAllConnections(@PathVariable("id") URI id) {
        return nodeService.getAllConnections(id);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deletes a single node by id",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        nodeService.delete(id);
    }

    @GetMapping("/{id}/resources")
    @Operation(
            summary = "Gets all resources for the given node",
            tags = {"nodes"})
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getResources(
            @Parameter(name = "id", required = true) @PathVariable("id") URI nodeId,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    Optional<String> language,
            @Parameter(description = "Include all contexts") @RequestParam(value = "includeContexts", required = false)
                    Optional<Boolean> includeContexts,
            @Parameter(description = "If true, resources from children are fetched recursively")
                    @RequestParam(value = "recursive", required = false, defaultValue = "false")
                    boolean recursive,
            @Parameter(
                            description =
                                    "Select by resource type id(s). If not specified, resources of all types will be returned. "
                                            + "Multiple ids may be separated with comma or the parameter may be repeated for each id.")
                    @RequestParam(value = "type", required = false)
                    URI[] resourceTypeIds,
            @Parameter(description = "Select by relevance. If not specified, all resources will be returned.")
                    @RequestParam(value = "relevance", required = false)
                    URI relevance) {
        final Set<URI> resourceTypeIdSet;

        if (resourceTypeIds == null) {
            resourceTypeIdSet = Set.of();
        } else {
            resourceTypeIdSet = new HashSet<>(Arrays.asList(resourceTypeIds));
        }

        return nodeService.getResourcesByNodeId(
                nodeId, resourceTypeIdSet, relevance, language, recursive, includeContexts);
    }

    @GetMapping("{id}/full")
    @Operation(summary = "Gets node including information about all parents, and resourceTypes for this resource")
    @Transactional(readOnly = true)
    public NodeWithParents getNodeFull(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    Optional<String> language,
            @Parameter(description = "Include all contexts") @RequestParam(value = "includeContexts", required = false)
                    Optional<Boolean> includeContexts) {
        var node = nodeService.getNode(id);
        return new NodeWithParents(node, language.orElse(Constants.DefaultLanguage), includeContexts);
    }

    @PutMapping("/{id}/makeResourcesPrimary")
    @Operation(
            summary = "Makes all connected resources primary",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    public ResponseEntity<Boolean> makeResourcesPrimary(
            @Parameter(name = "id", required = true) @PathVariable("id") URI nodeId,
            @Parameter(description = "If true, children are fetched recursively")
                    @RequestParam(value = "recursive", required = false, defaultValue = "false")
                    boolean recursive) {
        return ResponseEntity.of(Optional.of(nodeService.makeAllResourcesPrimary(nodeId, recursive)));
    }

    @PostMapping("{id}/clone")
    @Operation(
            summary = "Clones a node, presumably a resource, including resource-types and translations",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> cloneResource(
            @Parameter(name = "id", description = "Id of node to clone", example = "urn:resource:1") @PathVariable("id")
                    URI publicId,
            @Parameter(name = "node", description = "Object containing contentUri. Other values are ignored.")
                    @RequestBody
                    @Schema(name = "NodePOST")
                    NodePostPut command) {
        var entity = nodeService.cloneNode(publicId, command.contentUri);
        URI location = URI.create(getLocation() + "/" + entity.getPublicId());
        return ResponseEntity.created(location).build();
    }
}
