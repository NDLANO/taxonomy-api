/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.dtos.NodeResourceDTO;
import no.ndla.taxonomy.service.ContextUpdaterService;
import no.ndla.taxonomy.service.NodeConnectionService;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/node-resources" })
public class NodeResources extends CrudControllerWithMetadata<NodeConnection> {

    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public NodeResources(NodeRepository nodeRepository, NodeConnectionService connectionService,
            NodeConnectionRepository nodeConnectionRepository, RelevanceRepository relevanceRepository,
            ContextUpdaterService cachedUrlUpdaterService) {
        super(nodeConnectionRepository, cachedUrlUpdaterService);
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeRepository = nodeRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @Operation(summary = "Gets all connections between node and resources")
    @Transactional(readOnly = true)
    public List<NodeResourceDTO> getAllNodeResources() {
        return nodeConnectionRepository.findAllByChildNodeType(NodeType.RESOURCE).stream().map(NodeResourceDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between node and resources paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeResourceDTO> getNodeResourcesPage(
            @Parameter(name = "page", description = "The page to fetch", required = true) Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch", required = true) Optional<Integer> pageSize) {

        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page.get() - 1, pageSize.get());
        var connections = nodeConnectionRepository.findIdsPaginatedByChildNodeType(pageRequest, NodeType.RESOURCE);
        var ids = connections.stream().map(DomainEntity::getId).toList();
        var results = nodeConnectionRepository.findByIds(ids);
        var contents = results.stream().map(NodeResourceDTO::new).collect(Collectors.toList());
        return new SearchResultDTO<>(connections.getTotalElements(), page.get(), pageSize.get(), contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a specific connection between a node and a resource")
    @Transactional(readOnly = true)
    public NodeResourceDTO getNodeResource(@PathVariable("id") URI id) {
        NodeConnection connection = nodeConnectionRepository.getByPublicId(id);
        return new NodeResourceDTO(connection);
    }

    @PostMapping
    @Operation(summary = "Adds a resource to a node", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createNodeResource(
            @Parameter(name = "connection", description = "new node/resource connection ") @RequestBody NodeResourcePOST command) {
        var parent = nodeRepository.getByPublicId(command.nodeId);
        var child = nodeRepository.getByPublicId(command.resourceId);
        var relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId) : null;
        var rank = command.rank == 0 ? null : command.rank;
        var primary = Optional.of(command.primary);

        final var nodeConnection = connectionService.connectParentChild(parent, child, relevance, rank, primary);

        var location = URI.create("/node-resources/" + nodeConnection.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Removes a resource from a node", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        var connection = nodeConnectionRepository.getByPublicId(id);
        connectionService.disconnectParentChildConnection(connection);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a connection between a node and a resource", description = "Use to update which node is primary to the resource or to change sorting order.", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateNodeResource(@PathVariable("id") URI id,
            @Parameter(name = "connection", description = "Updated node/resource connection") @RequestBody NodeResourcePUT command) {
        final var nodeResource = nodeConnectionRepository.getByPublicId(id);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        if (nodeResource.isPrimary().orElse(false) && !command.primary) {
            throw new PrimaryParentRequiredException();
        }

        connectionService.updateParentChild(nodeResource, relevance, command.rank > 0 ? command.rank : null,
                Optional.empty());
    }

    public static class NodeResourcePOST {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Node id", example = "urn:node:345")
        public URI nodeId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource id", example = "urn:resource:345")
        public URI resourceId;

        @JsonProperty
        @Schema(description = "Primary connection", example = "true")
        public boolean primary = true;

        @JsonProperty
        @Schema(description = "Order in which resource is sorted for the node", example = "1")
        public int rank;

        @JsonProperty
        @Schema(description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class NodeResourcePUT {
        @JsonProperty
        @Schema(description = "Node resource connection id", example = "urn:node-resource:123")
        public URI id;

        @JsonProperty
        @Schema(description = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @Schema(description = "Order in which the resource will be sorted for this node.", example = "1")
        public int rank;

        @JsonProperty
        @Schema(description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }
}
