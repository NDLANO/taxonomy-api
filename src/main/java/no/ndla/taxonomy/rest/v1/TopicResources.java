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
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
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
@RequestMapping(path = { "/v1/topic-resources" })
public class TopicResources {

    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public TopicResources(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            NodeConnectionService connectionService, RelevanceRepository relevanceRepository) {
        this.nodeRepository = nodeRepository;
        this.connectionService = connectionService;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @Operation(summary = "Gets all connections between topics and resources")
    @Transactional(readOnly = true)
    public List<TopicResourceDTO> getAllTopicResources() {
        return nodeConnectionRepository.findAllByChildNodeType(NodeType.RESOURCE).stream().map(TopicResourceDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between topic and resources paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<TopicResourceDTO> getTopicResourcePage(
            @Parameter(name = "page", description = "The page to fetch", required = true) Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch", required = true) Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page.get() - 1, pageSize.get());
        var connections = nodeConnectionRepository.findIdsPaginatedByChildNodeType(pageRequest, NodeType.RESOURCE);
        var ids = connections.stream().map(DomainEntity::getId).collect(Collectors.toList());
        var results = nodeConnectionRepository.findByIds(ids);
        var contents = results.stream().map(TopicResourceDTO::new).collect(Collectors.toList());
        return new SearchResultDTO<>(connections.getTotalElements(), page.get(), pageSize.get(), contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a specific connection between a topic and a resource")
    @Transactional(readOnly = true)
    public TopicResourceDTO getTopicResource(@PathVariable("id") URI id) {
        var resourceConnection = nodeConnectionRepository.getByPublicId(id);
        return new TopicResourceDTO(resourceConnection);
    }

    @PostMapping
    @Operation(summary = "Adds a resource to a topic", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createTopicResource(
            @Parameter(name = "connection", description = "new topic/resource connection ") @RequestBody TopicResourcePOST command) {

        Node topic = nodeRepository.getByPublicId(command.topicid);
        Node resource = nodeRepository.getByPublicId(command.resourceId);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        var primary = Optional.of(command.primary);
        var rank = command.rank == 0 ? null : command.rank;

        final NodeConnection topicResource;
        topicResource = connectionService.connectParentChild(topic, resource, relevance, rank, primary);

        URI location = URI.create("/topic-resources/" + topicResource.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Removes a resource from a topic", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteTopicResource(@PathVariable("id") URI id) {
        var connection = nodeConnectionRepository.getByPublicId(id);
        connectionService.disconnectParentChildConnection(connection);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a connection between a topic and a resource", description = "Use to update which topic is primary to the resource or to change sorting order.", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateTopicResource(@PathVariable("id") URI id,
            @Parameter(name = "connection", description = "Updated topic/resource connection") @RequestBody TopicResourcePUT command) {
        var topicResource = nodeConnectionRepository.getByPublicId(id);
        var relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId) : null;

        if (topicResource.isPrimary().orElse(false) && !command.primary) {
            throw new PrimaryParentRequiredException();
        }
        var rank = command.rank > 0 ? command.rank : null;
        var primary = Optional.of(command.primary);

        connectionService.updateParentChild(topicResource, relevance, rank, primary);
    }

    public static class TopicResourcePOST {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource id", example = "urn:resource:345")
        public URI resourceId;

        @JsonProperty
        @Schema(description = "Primary connection", example = "true")
        public boolean primary = true;

        @JsonProperty
        @Schema(description = "Order in which resource is sorted for the topic", example = "1")
        public int rank;

        @JsonProperty
        @Schema(description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class TopicResourcePUT {
        @JsonProperty
        @Schema(description = "Topic resource connection id", example = "urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @Schema(description = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @Schema(description = "Order in which the resource will be sorted for this topic.", example = "1")
        public int rank;

        @JsonProperty
        @Schema(description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    @Schema(name = "TopicResource")
    public static class TopicResourceDTO {

        @JsonProperty
        @Schema(description = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @Schema(description = "Resource id", example = "urn:resource:345")
        URI resourceId;

        @JsonProperty
        @Schema(description = "Topic resource connection id", example = "urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @Schema(description = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @Schema(description = "Order in which the resource is sorted for the topic", example = "1")
        public int rank;

        @JsonProperty
        @Schema(description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        TopicResourceDTO(NodeConnection topicResource) {
            id = topicResource.getPublicId();
            topicResource.getParent().ifPresent(topic -> topicid = topic.getPublicId());
            topicResource.getResource().ifPresent(resource -> resourceId = resource.getPublicId());
            primary = topicResource.isPrimary().orElse(false);
            rank = topicResource.getRank();
            relevanceId = topicResource.getRelevance().map(Relevance::getPublicId).orElse(null);
        }

        TopicResourceDTO() {

        }

    }
}
