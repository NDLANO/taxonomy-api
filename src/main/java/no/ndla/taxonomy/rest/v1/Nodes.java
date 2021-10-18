package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.EntityWithPathConnection;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.NodeCommand;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/nodes" })
public class Nodes extends CrudControllerWithMetadata<Node> {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeService nodeService;
    private final ResourceService resourceService;
    private final RecursiveNodeTreeService recursiveNodeTreeService;
    private final TreeSorter treeSorter;

    public Nodes(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            NodeService nodeService, CachedUrlUpdaterService cachedUrlUpdaterService, ResourceService resourceService,
            RecursiveNodeTreeService recursiveNodeTreeService, TreeSorter treeSorter,
            MetadataApiService metadataApiService, MetadataUpdateService metadataUpdateService) {
        super(nodeRepository, cachedUrlUpdaterService, metadataApiService, metadataUpdateService);

        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeService = nodeService;
        this.recursiveNodeTreeService = recursiveNodeTreeService;
        this.resourceService = resourceService;
        this.treeSorter = treeSorter;
    }

    @GetMapping
    @ApiOperation("Gets all nodes")
    public List<EntityWithPathDTO> index(
            @ApiParam(value = "Filter by nodeType") @RequestParam(value = "nodeType", required = false) NodeType nodeTypeFilter,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language,
            @ApiParam(value = "Filter by contentUri") @RequestParam(value = "contentURI", required = false) URI contentUriFilter,
            @ApiParam(value = "Only root level") @RequestParam(value = "isRoot", required = false) boolean isRoot,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "key", required = false) String key,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "value", required = false) String value) {

        if (contentUriFilter != null && contentUriFilter.toString().equals("")) {
            contentUriFilter = null;
        }
        if (key != null) {
            return nodeService.getNodes(language, nodeTypeFilter, contentUriFilter,
                    new MetadataKeyValueQuery(key, value));
        }
        return nodeService.getNodes(language, nodeTypeFilter, contentUriFilter, isRoot);
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single node")
    @Transactional
    @InjectMetadata
    public NodeDTO get(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return new NodeDTO(nodeRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id)
                .orElseThrow(() -> new NotFoundHttpResponseException("Node was not found")), language);
    }

    @PostMapping
    @ApiOperation(value = "Creates a new node")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new node") @RequestBody NodeCommand command) {
        return doPost(new Node(command.nodeType), command);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a single node")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(@PathVariable("id") URI id,
            @ApiParam(name = "node", value = "The updated node. Fields not included will be set to null.") @RequestBody NodeCommand command) {
        doPut(id, command);
    }

    @GetMapping("/{id}/nodes")
    @ApiOperation(value = "Gets all children for this node")
    @InjectMetadata
    public List<EntityWithPathChildDTO> getChildren(@ApiParam(value = "id", required = true) @PathVariable("id") URI id,
            @ApiParam("If true, children are fetched recursively") @RequestParam(value = "recursive", required = false, defaultValue = "false") boolean recursive,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        final var node = nodeRepository.findFirstByPublicId(id).orElseThrow(() -> new NotFoundException("Node", id));

        final List<Integer> childrenIds;
        if (recursive) {
            childrenIds = recursiveNodeTreeService.getRecursiveNodes(node).stream()
                    .map(RecursiveNodeTreeService.TreeElement::getId).collect(Collectors.toList());
        } else {
            childrenIds = node.getChildConnections().stream().map(EntityWithPathConnection::getConnectedChild)
                    .filter(Optional::isPresent).map(Optional::get).map(EntityWithPath::getId)
                    .collect(Collectors.toList());
        }
        final var children = nodeConnectionRepository
                .findAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(childrenIds);

        final var returnList = new ArrayList<EntityWithPathChildDTO>();

        children.stream().map(nodeConnection -> new NodeChildDTO(node, nodeConnection, language))
                .forEach(returnList::add);

        return treeSorter.sortList(returnList).stream().distinct().collect(Collectors.toList());
    }

    @GetMapping("/{id}/connections")
    @ApiOperation(value = "Gets all parents and children this node is connected to")
    public List<ConnectionIndexDTO> getAllConnections(@PathVariable("id") URI id) {
        return nodeService.getAllConnections(id);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single node by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        nodeService.delete(id);
    }

    @GetMapping("/{id}/resources")
    @ApiOperation(value = "Gets all resources for the given node", tags = { "nodes" })
    public List<ResourceWithNodeConnectionDTO> getResources(
            @ApiParam(value = "id", required = true) @PathVariable("id") URI nodeId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false) String language,
            @ApiParam("If true, resources from children are fetched recursively") @RequestParam(value = "recursive", required = false, defaultValue = "false") boolean recursive,
            @ApiParam(value = "Select by resource type id(s). If not specified, resources of all types will be returned."
                    + "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true) @RequestParam(value = "type", required = false) URI[] resourceTypeIds,
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.") @RequestParam(value = "relevance", required = false) URI relevance) {
        final Set<URI> resourceTypeIdSet;

        if (resourceTypeIds == null) {
            resourceTypeIdSet = Set.of();
        } else {
            resourceTypeIdSet = new HashSet<>(Arrays.asList(resourceTypeIds));
        }

        return resourceService.getResourcesByNodeId(nodeId, resourceTypeIdSet, relevance, language, recursive);
    }

}
