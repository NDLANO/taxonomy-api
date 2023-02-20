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
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.dtos.nodes.NodeConnectionPage;
import no.ndla.taxonomy.rest.v1.dtos.nodes.ParentChildIndexDocument;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.EntityConnectionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/node-connections" })
public class NodeConnections extends CrudControllerWithMetadata<NodeConnection> {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public NodeConnections(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService, RelevanceRepository relevanceRepository,
            CachedUrlUpdaterService cachedUrlUpdaterService) {
        super(nodeConnectionRepository, cachedUrlUpdaterService);
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @Operation(summary = "Gets all connections between node and children")
    public List<ParentChildIndexDocument> index() {
        return nodeConnectionRepository.findAllIncludingParentAndChild().stream().map(ParentChildIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between node and children paginated")
    public NodeConnectionPage allPaginated(
            @Parameter(name = "page", description = "The page to fetch") Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch") Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeConnectionRepository.findIdsPaginated(PageRequest.of(page.get() - 1, pageSize.get()));
        var results = nodeConnectionRepository.findByIds(ids.getContent());
        var contents = results.stream().map(ParentChildIndexDocument::new).collect(Collectors.toList());
        return new NodeConnectionPage(ids.getTotalElements(), contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single connection between a node and a child")
    public ParentChildIndexDocument get(@PathVariable("id") URI id) {
        NodeConnection topicSubtopic = nodeConnectionRepository.getByPublicId(id);
        return new ParentChildIndexDocument(topicSubtopic);
    }

    @PostMapping
    @Operation(summary = "Adds a node to a parent", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @Parameter(name = "connection", description = "The new connection") @RequestBody AddChildToParentCommand command) {
        Node parent = nodeRepository.getByPublicId(command.parentId);
        Node child = nodeRepository.getByPublicId(command.childId);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        final var nodeConnection = connectionService.connectParentChild(parent, child, relevance,
                command.rank == 0 ? null : command.rank, Optional.empty());

        URI location = URI.create("/node-child/" + nodeConnection.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Removes a connection between a node and a child", security = {
            @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        var connection = nodeConnectionRepository.getByPublicId(id);
        connectionService.disconnectParentChildConnection(connection);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Updates a connection between a node and a child", description = "Use to update which node is primary to a child or to alter sorting order", security = {
            @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @Parameter(name = "connection", description = "The updated connection") @RequestBody UpdateNodeChildCommand command) {
        final var connection = nodeConnectionRepository.getByPublicId(id);
        var relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId) : null;
        var rank = command.rank > 0 ? command.rank : null;
        if (connection.isPrimary().orElse(false) && !command.primary) {
            throw new PrimaryParentRequiredException();
        }
        var primary = Optional.of(command.primary);

        connectionService.updateParentChild(connection, relevance, rank, primary);
    }

    public static class AddChildToParentCommand {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, title = "Parent id", example = "urn:topic:234")
        public URI parentId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Child id", example = "urn:topic:234")
        public URI childId;

        @JsonProperty
        @Schema(description = "Backwards compatibility: Always true. Ignored on insert/update", example = "true")
        public boolean primary = true;

        @JsonProperty
        @Schema(description = "Order in which to sort the child for the parent", example = "1")
        public int rank;

        @JsonProperty
        @Schema(description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateNodeChildCommand {
        @JsonProperty
        @Schema(description = "Connection id", example = "urn:node-has-child:345")
        public URI id;

        @JsonProperty
        @Schema(description = "Backwards compatibility: Always true. Ignored on insert/update", example = "true")
        public boolean primary;

        @JsonProperty
        @Schema(description = "Order in which subtopic is sorted for the topic", example = "1")
        public int rank;

        @JsonProperty
        @Schema(description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

}
