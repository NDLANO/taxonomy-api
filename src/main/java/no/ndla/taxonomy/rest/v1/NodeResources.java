/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.rest.v1.dtos.nodes.NodeConnectionPage;
import no.ndla.taxonomy.rest.v1.dtos.nodes.ParentChildIndexDocument;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.EntityConnectionService;
import no.ndla.taxonomy.service.MetadataService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/node-resources" })
@Transactional
public class NodeResources extends CrudControllerWithMetadata<NodeConnection> {

    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public NodeResources(NodeRepository nodeRepository, EntityConnectionService connectionService,
            NodeConnectionRepository nodeConnectionRepository, RelevanceRepository relevanceRepository,
            CachedUrlUpdaterService cachedUrlUpdaterService, MetadataService metadataService) {
        super(nodeConnectionRepository, cachedUrlUpdaterService, metadataService);
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeRepository = nodeRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @ApiOperation(value = "Gets all connections between node and resources")
    public List<ParentChildIndexDocument> index() {
        return nodeConnectionRepository.findAllByChildNodeType(NodeType.RESOURCE).stream()
                .map(ParentChildIndexDocument::new).collect(Collectors.toList());
    }

    @GetMapping("/page")
    @ApiOperation(value = "Gets all connections between node and resources paginated")
    public NodeConnectionPage allPaginated(
            @ApiParam(name = "page", value = "The page to fetch", required = true) Optional<Integer> page,
            @ApiParam(name = "pageSize", value = "Size of page to fetch", required = true) Optional<Integer> pageSize) {

        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page.get() - 1, pageSize.get());
        var ids = nodeConnectionRepository.findIdsPaginatedByChildNodeType(pageRequest, NodeType.RESOURCE);
        var results = nodeConnectionRepository.findByIds(ids.getContent());
        var contents = results.stream().map(ParentChildIndexDocument::new).collect(Collectors.toList());
        return new NodeConnectionPage(ids.getTotalElements(), contents);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a specific connection between a node and a resource")
    public ParentChildIndexDocument get(@PathVariable("id") URI id) {
        NodeConnection connection = nodeConnectionRepository.getByPublicId(id);
        return new ParentChildIndexDocument(connection);
    }

    @PostMapping
    @ApiOperation(value = "Adds a resource to a node")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "new node/resource connection ") @RequestBody AddResourceToNodeCommand command) {
        var parent = nodeRepository.getByPublicId(command.nodeId);
        var child = nodeRepository.getByPublicId(command.resourceId);
        var relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId) : null;
        var rank = command.rank == 0 ? null : command.rank;
        var primary = Optional.of(command.primary);

        final var nodeConnection = connectionService.connectParentChild(parent, child, relevance, rank, primary);

        var location = URI.create("/node-child/" + nodeConnection.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Removes a resource from a node")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectAllParents(id);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a connection between a node and a resource", notes = "Use to update which node is primary to the resource or to change sorting order.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @ApiParam(name = "connection", value = "Updated node/resource connection") @RequestBody UpdateNodeResourceCommand command) {
        final var nodeResource = nodeConnectionRepository.getByPublicId(id);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        if (nodeResource.isPrimary().orElse(false) && !command.primary) {
            throw new PrimaryParentRequiredException();
        }

        connectionService.updateParentChild(nodeResource, relevance, command.rank > 0 ? command.rank : null,
                Optional.empty());
    }

    public static class AddResourceToNodeCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Node id", example = "urn:node:345")
        public URI nodeId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:345")
        public URI resourceId;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary = true;

        @JsonProperty
        @ApiModelProperty(value = "Order in which resource is sorted for the node", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateNodeResourceCommand {
        @JsonProperty
        @ApiModelProperty(value = "Node resource connection id", example = "urn:node-resource:123")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the resource will be sorted for this node.", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }
}
