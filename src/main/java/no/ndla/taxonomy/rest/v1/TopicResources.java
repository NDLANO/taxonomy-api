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
import no.ndla.taxonomy.service.EntityConnectionService;
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
@RequestMapping(path = { "/v1/topic-resources" })
@Transactional
public class TopicResources {

    private final NodeRepository nodeRepository;
    private final ResourceRepository resourceRepository;
    private final NodeResourceRepository nodeResourceRepository;
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public TopicResources(NodeRepository nodeRepository, ResourceRepository resourceRepository,
            NodeResourceRepository nodeResourceRepository, EntityConnectionService connectionService,
            RelevanceRepository relevanceRepository) {
        this.nodeRepository = nodeRepository;
        this.resourceRepository = resourceRepository;
        this.nodeResourceRepository = nodeResourceRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @Operation(summary = "Gets all connections between topics and resources")
    public List<TopicResourceIndexDocument> index() {
        return nodeResourceRepository.findAllIncludingNodeAndResource().stream().map(TopicResourceIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between topic and resources paginated")
    public TopicResourcePage allPaginated(
            @Parameter(name = "page", description = "The page to fetch", required = true) Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch", required = true) Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeResourceRepository.findIdsPaginated(PageRequest.of(page.get() - 1, pageSize.get()));
        var results = nodeResourceRepository.findByIds(ids.getContent());
        var contents = results.stream().map(TopicResourceIndexDocument::new).collect(Collectors.toList());
        return new TopicResourcePage(ids.getTotalElements(), contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a specific connection between a topic and a resource")
    public TopicResourceIndexDocument get(@PathVariable("id") URI id) {
        NodeResource topicResource = nodeResourceRepository.getByPublicId(id);
        return new TopicResourceIndexDocument(topicResource);
    }

    @PostMapping
    @Operation(summary = "Adds a resource to a topic", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @Parameter(name = "connection", description = "new topic/resource connection ") @RequestBody AddResourceToTopicCommand command) {

        Node topic = nodeRepository.getByPublicId(command.topicid);
        Resource resource = resourceRepository.getByPublicId(command.resourceId);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        final NodeResource topicResource;
        topicResource = connectionService.connectNodeResource(topic, resource, relevance, command.primary,
                command.rank == 0 ? null : command.rank);

        URI location = URI.create("/topic-resources/" + topicResource.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Removes a resource from a topic", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectNodeResource(nodeResourceRepository.getByPublicId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a connection between a topic and a resource", description = "Use to update which topic is primary to the resource or to change sorting order.", security = {
            @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @Parameter(name = "connection", description = "Updated topic/resource connection") @RequestBody UpdateTopicResourceCommand command) {
        NodeResource topicResource = nodeResourceRepository.getByPublicId(id);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        if (topicResource.isPrimary().orElse(false) && !command.primary) {
            throw new PrimaryParentRequiredException();
        }

        connectionService.updateNodeResource(topicResource, relevance, command.primary,
                command.rank > 0 ? command.rank : null);
    }

    public static class AddResourceToTopicCommand {
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

    public static class UpdateTopicResourceCommand {
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

    public static class TopicResourcePage {
        @JsonProperty
        @Schema(description = "Total number of elements")
        public long totalCount;

        @JsonProperty
        @Schema(description = "Page containing results")
        public List<TopicResourceIndexDocument> results;

        TopicResourcePage() {
        }

        TopicResourcePage(long totalCount, List<TopicResourceIndexDocument> results) {
            this.totalCount = totalCount;
            this.results = results;
        }
    }

    public static class TopicResourceIndexDocument {

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

        TopicResourceIndexDocument() {
        }

        TopicResourceIndexDocument(NodeResource topicResource) {
            id = topicResource.getPublicId();
            topicResource.getNode().ifPresent(topic -> topicid = topic.getPublicId());
            topicResource.getResource().ifPresent(resource -> resourceId = resource.getPublicId());
            primary = topicResource.isPrimary().orElse(false);
            rank = topicResource.getRank();
            relevanceId = topicResource.getRelevance().map(Relevance::getPublicId).orElse(null);
        }
    }
}
