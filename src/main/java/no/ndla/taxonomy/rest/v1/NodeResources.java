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
import no.ndla.taxonomy.domain.NodeResource;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.NodeResourceRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.EntityConnectionService;
import no.ndla.taxonomy.service.MetadataService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
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
public class NodeResources extends CrudControllerWithMetadata<NodeResource> {

    private final NodeRepository nodeRepository;
    private final ResourceRepository resourceRepository;
    private final NodeResourceRepository nodeResourceRepository;
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public NodeResources(NodeRepository nodeRepository, ResourceRepository resourceRepository,
            NodeResourceRepository nodeResourceRepository, EntityConnectionService connectionService,
            RelevanceRepository relevanceRepository, CachedUrlUpdaterService cachedUrlUpdaterService,
            MetadataService metadataService) {
        super(nodeResourceRepository, cachedUrlUpdaterService, metadataService);
        this.nodeRepository = nodeRepository;
        this.resourceRepository = resourceRepository;
        this.nodeResourceRepository = nodeResourceRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @Operation(summary = "Gets all connections between node and resources")
    public List<NodeResourceDto> index() {
        return nodeResourceRepository.findAllIncludingNodeAndResource().stream().map(NodeResourceDto::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between node and resources paginated")
    public NodeResourceDtoPage allPaginated(
            @Parameter(name = "page", description = "The page to fetch", required = true) Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch", required = true) Optional<Integer> pageSize) {

        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeResourceRepository.findIdsPaginated(PageRequest.of(page.get() - 1, pageSize.get()));
        var results = nodeResourceRepository.findByIds(ids.getContent());
        var contents = results.stream().map(NodeResourceDto::new).collect(Collectors.toList());
        return new NodeResourceDtoPage(ids.getTotalElements(), contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a specific connection between a node and a resource")
    public NodeResourceDto get(@PathVariable("id") URI id) {
        NodeResource topicResource = nodeResourceRepository.getByPublicId(id);
        return new NodeResourceDto(topicResource);
    }

    @PostMapping
    @Operation(summary = "Adds a resource to a node", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @Parameter(name = "connection", description = "new node/resource connection ") @RequestBody AddResourceToNodeCommand command) {

        Node node = nodeRepository.getByPublicId(command.nodeId);
        Resource resource = resourceRepository.getByPublicId(command.resourceId);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        final NodeResource nodeResource;
        nodeResource = connectionService.connectNodeResource(node, resource, relevance, command.primary,
                command.rank == 0 ? null : command.rank);

        URI location = URI.create("/node-resources/" + nodeResource.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Removes a resource from a node", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectNodeResource(nodeResourceRepository.getByPublicId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a connection between a node and a resource", description = "Use to update which node is primary to the resource or to change sorting order.", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @Parameter(name = "connection", description = "Updated node/resource connection") @RequestBody UpdateNodeResourceCommand command) {
        NodeResource nodeResource = nodeResourceRepository.getByPublicId(id);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        if (nodeResource.isPrimary().orElse(false) && !command.primary) {
            throw new PrimaryParentRequiredException();
        }

        connectionService.updateNodeResource(nodeResource, relevance, command.primary,
                command.rank > 0 ? command.rank : null);
    }

    public static class AddResourceToNodeCommand {
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

    public static class UpdateNodeResourceCommand {
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

    public static class NodeResourceDtoPage {
        @JsonProperty
        @Schema(description = "Total number of elements")
        public long totalCount;

        @JsonProperty
        @Schema(description = "Page containing results")
        public List<NodeResourceDto> results;

        NodeResourceDtoPage() {
        }

        NodeResourceDtoPage(long totalCount, List<NodeResourceDto> results) {
            this.totalCount = totalCount;
            this.results = results;
        }
    }

    public static class NodeResourceDto {

        @JsonProperty
        @Schema(description = "Node id", example = "urn:node:345")
        public URI nodeId;

        @JsonProperty
        @Schema(description = "Resource id", example = "urn:resource:345")
        URI resourceId;

        @JsonProperty
        @Schema(description = "Node resource connection id", example = "urn:node-resource:123")
        public URI id;

        @JsonProperty
        @Schema(description = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @Schema(description = "Order in which the resource is sorted for the node", example = "1")
        public int rank;

        @JsonProperty
        @Schema(description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        @JsonProperty
        @Schema(description = "Metadata for entity. Read only.")
        private MetadataDto metadata;

        NodeResourceDto() {
        }

        NodeResourceDto(NodeResource nodeResource) {
            id = nodeResource.getPublicId();
            nodeResource.getNode().ifPresent(node -> nodeId = node.getPublicId());
            nodeResource.getResource().ifPresent(resource -> resourceId = resource.getPublicId());
            primary = nodeResource.isPrimary().orElse(false);
            rank = nodeResource.getRank();
            relevanceId = nodeResource.getRelevance().map(Relevance::getPublicId).orElse(null);
            metadata = new MetadataDto(nodeResource.getMetadata());
        }
    }
}
