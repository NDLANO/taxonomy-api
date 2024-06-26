/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.dtos.NodeConnectionDTO;
import no.ndla.taxonomy.rest.v1.dtos.NodeConnectionPOST;
import no.ndla.taxonomy.rest.v1.dtos.NodeConnectionPUT;
import no.ndla.taxonomy.service.ContextUpdaterService;
import no.ndla.taxonomy.service.NodeConnectionService;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/node-connections", "/v1/node-connections/"})
public class NodeConnections extends CrudControllerWithMetadata<NodeConnection> {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;
    private final NodeService nodeService;

    public NodeConnections(
            NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository,
            NodeConnectionService connectionService,
            RelevanceRepository relevanceRepository,
            ContextUpdaterService contextUpdaterService,
            NodeService nodeService) {
        super(nodeConnectionRepository, contextUpdaterService, nodeService);
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
        this.nodeService = nodeService;
    }

    @GetMapping
    @Operation(summary = "Gets all connections between node and children")
    @Transactional(readOnly = true)
    public List<NodeConnectionDTO> getAllNodeConnections() {
        final List<NodeConnectionDTO> listToReturn = new ArrayList<>();
        var ids = nodeConnectionRepository.findAllIds();
        final var counter = new AtomicInteger();
        ids.stream()
                .collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000))
                .values()
                .forEach(idChunk -> {
                    final var connections = nodeConnectionRepository.findByIds(idChunk);
                    var dtos = connections.stream().map(NodeConnectionDTO::new).toList();
                    listToReturn.addAll(dtos);
                });

        return listToReturn;
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between node and children paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeConnectionDTO> getNodeConnectionsPage(
            @Parameter(name = "page", description = "The page to fetch") Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch") Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1) throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeConnectionRepository.findIdsPaginated(PageRequest.of(page.get() - 1, pageSize.get()));
        var results = nodeConnectionRepository.findByIds(ids.getContent());
        var contents = results.stream().map(NodeConnectionDTO::new).collect(Collectors.toList());
        return new SearchResultDTO<>(ids.getTotalElements(), page.get(), pageSize.get(), contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single connection between a node and a child")
    @Transactional(readOnly = true)
    public NodeConnectionDTO getNodeConnection(@PathVariable("id") URI id) {
        NodeConnection topicSubtopic = nodeConnectionRepository.getByPublicId(id);
        return new NodeConnectionDTO(topicSubtopic);
    }

    @PostMapping
    @Operation(
            summary = "Adds a node to a parent",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createNodeConnection(
            @Parameter(name = "connection", description = "The new connection") @RequestBody
                    NodeConnectionPOST command) {
        Node parent = nodeRepository.getByPublicId(command.parentId);
        Node child = nodeRepository.getByPublicId(command.childId);
        var relevance = relevanceRepository.getByPublicId(command.relevanceId.orElse(URI.create("urn:relevance:core")));
        var rank = command.rank.orElse(null);
        final var nodeConnection =
                connectionService.connectParentChild(parent, child, relevance, rank, command.primary);

        URI location = URI.create("/node-child/" + nodeConnection.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Removes a connection between a node and a child",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        var connection = nodeConnectionRepository.getByPublicId(id);
        connectionService.disconnectParentChildConnection(connection);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Updates a connection between a node and a child",
            description = "Use to update which node is primary to a child or to alter sorting order",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateNodeConnection(
            @PathVariable("id") URI id,
            @Parameter(name = "connection", description = "The updated connection") @RequestBody
                    NodeConnectionPUT command) {
        final var connection = nodeConnectionRepository.getByPublicId(id);
        var relevance = relevanceRepository.getByPublicId(command.relevanceId.orElse(URI.create("urn:relevance:core")));
        if (connection.isPrimary().orElse(false) && !command.primary.orElse(false)) {
            throw new PrimaryParentRequiredException();
        }

        connectionService.updateParentChild(connection, relevance, command.rank, command.primary);
    }
}
