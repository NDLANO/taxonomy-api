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
@RequestMapping(path = { "/v1/node-connections" })
@Transactional
public class NodeConnections extends CrudControllerWithMetadata<NodeConnection> {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public NodeConnections(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService, RelevanceRepository relevanceRepository,
            CachedUrlUpdaterService cachedUrlUpdaterService, MetadataService metadataService) {
        super(nodeConnectionRepository, cachedUrlUpdaterService, metadataService);
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @ApiOperation(value = "Gets all connections between node and children")
    public List<ParentChildIndexDocument> index() {
        return nodeConnectionRepository.findAllIncludingParentAndChild().stream().map(ParentChildIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @ApiOperation(value = "Gets all connections between node and children paginated")
    public NodeConnectionPage allPaginated(@ApiParam(name = "page", value = "The page to fetch") Optional<Integer> page,
            @ApiParam(name = "pageSize", value = "Size of page to fetch") Optional<Integer> pageSize) {
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
    @ApiOperation(value = "Gets a single connection between a node and a child")
    public ParentChildIndexDocument get(@PathVariable("id") URI id) {
        NodeConnection topicSubtopic = nodeConnectionRepository.getByPublicId(id);
        return new ParentChildIndexDocument(topicSubtopic);
    }

    @PostMapping
    @ApiOperation(value = "Adds a node to a parent")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new connection") @RequestBody AddChildToParentCommand command) {
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
    @ApiOperation(value = "Removes a connection between a node and a child")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectParentChildConnection(nodeConnectionRepository.getByPublicId(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Updates a connection between a node and a child", notes = "Use to update which node is primary to a child or to alter sorting order")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @ApiParam(name = "connection", value = "The updated connection") @RequestBody UpdateNodeChildCommand command) {
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
        @ApiModelProperty(required = true, value = "Parent id", example = "urn:topic:234")
        public URI parentId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Child id", example = "urn:topic:234")
        public URI childId;

        @JsonProperty
        @ApiModelProperty(value = "Backwards compatibility: Always true. Ignored on insert/update", example = "true")
        public boolean primary = true;

        @JsonProperty
        @ApiModelProperty(value = "Order in which to sort the child for the parent", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateNodeChildCommand {
        @JsonProperty
        @ApiModelProperty(value = "Connection id", example = "urn:node-has-child:345")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Backwards compatibility: Always true. Ignored on insert/update", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which subtopic is sorted for the topic", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

}
