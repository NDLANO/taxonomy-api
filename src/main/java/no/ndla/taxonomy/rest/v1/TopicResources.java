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
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.dtos.TopicResourceDTO;
import no.ndla.taxonomy.rest.v1.dtos.TopicResourcePOST;
import no.ndla.taxonomy.rest.v1.dtos.TopicResourcePUT;
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

    @Deprecated
    @GetMapping
    @Operation(summary = "Gets all connections between topics and resources")
    @Transactional(readOnly = true)
    public List<TopicResourceDTO> getAllTopicResources() {
        return nodeConnectionRepository.findAllByChildNodeType(NodeType.RESOURCE).stream().map(TopicResourceDTO::new)
                .collect(Collectors.toList());
    }

    @Deprecated
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

    @Deprecated
    @GetMapping("/{id}")
    @Operation(summary = "Gets a specific connection between a topic and a resource")
    @Transactional(readOnly = true)
    public TopicResourceDTO getTopicResource(@PathVariable("id") URI id) {
        var resourceConnection = nodeConnectionRepository.getByPublicId(id);
        return new TopicResourceDTO(resourceConnection);
    }

    @Deprecated
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

    @Deprecated
    @DeleteMapping("/{id}")
    @Operation(summary = "Removes a resource from a topic", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void deleteTopicResource(@PathVariable("id") URI id) {
        var connection = nodeConnectionRepository.getByPublicId(id);
        connectionService.disconnectParentChildConnection(connection);
    }

    @Deprecated
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

}
