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
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
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
@RequestMapping(path = { "/v1/topic-subtopics" })
@Transactional
public class TopicSubtopics {
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public TopicSubtopics(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService, RelevanceRepository relevanceRepository) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @Operation(summary = "Gets all connections between topics and subtopics")
    public List<TopicSubtopicIndexDocument> index() {
        return nodeConnectionRepository.findAllIncludingParentAndChild().stream().map(TopicSubtopicIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between topics and subtopics paginated")
    public TopicSubtopicPage allPaginated(
            @Parameter(name = "page", description = "The page to fetch") Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch") Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeConnectionRepository.findIdsPaginated(PageRequest.of(page.get() - 1, pageSize.get()));
        var results = nodeConnectionRepository.findByIds(ids.getContent());
        var contents = results.stream().map(TopicSubtopics.TopicSubtopicIndexDocument::new)
                .collect(Collectors.toList());
        return new TopicSubtopicPage(ids.getTotalElements(), contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single connection between a topic and a subtopic")
    public TopicSubtopicIndexDocument get(@PathVariable("id") URI id) {
        NodeConnection topicSubtopic = nodeConnectionRepository.getByPublicId(id);
        return new TopicSubtopicIndexDocument(topicSubtopic);
    }

    @PostMapping
    @Operation(summary = "Adds a subtopic to a topic", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @Parameter(name = "connection", description = "The new connection") @RequestBody AddSubtopicToTopicCommand command) {
        Node topic = nodeRepository.getByPublicId(command.topicid);
        Node subtopic = nodeRepository.getByPublicId(command.subtopicid);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        final var topicSubtopic = connectionService.connectParentChild(topic, subtopic, relevance,
                command.rank == 0 ? null : command.rank);

        URI location = URI.create("/topic-subtopics/" + topicSubtopic.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Removes a connection between a topic and a subtopic", security = {
            @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectParentChildConnection(nodeConnectionRepository.getByPublicId(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Updates a connection between a topic and a subtopic", description = "Use to update which topic is primary to a subtopic or to alter sorting order", security = {
            @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @Parameter(name = "connection", description = "The updated connection") @RequestBody UpdateTopicSubtopicCommand command) {
        final var topicSubtopic = nodeConnectionRepository.getByPublicId(id);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        connectionService.updateParentChild(topicSubtopic, relevance, command.rank > 0 ? command.rank : null);
    }

    public static class AddSubtopicToTopicCommand {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Subtopic id", example = "urn:topic:234")
        public URI subtopicid;

        @JsonProperty
        @Schema(description = "Backwards compatibility: Always true. Ignored on insert/update", example = "true")
        public boolean primary = true;

        @JsonProperty
        @Schema(description = "Order in which to sort the subtopic for the topic", example = "1")
        public int rank;

        @JsonProperty
        @Schema(description = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateTopicSubtopicCommand {
        @JsonProperty
        @Schema(description = "Connection id", example = "urn:topic-has-subtopics:345")
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

    public static class TopicSubtopicPage {
        @JsonProperty
        @Schema(description = "Total number of elements")
        public long totalCount;

        @JsonProperty
        @Schema(description = "Page containing results")
        public List<TopicSubtopicIndexDocument> results;

        TopicSubtopicPage() {
        }

        TopicSubtopicPage(long totalCount, List<TopicSubtopicIndexDocument> results) {
            this.totalCount = totalCount;
            this.results = results;
        }
    }

    public static class TopicSubtopicIndexDocument {
        @JsonProperty
        @Schema(description = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @Schema(description = "Subtopic id", example = "urn:topic:234")
        public URI subtopicid;

        @JsonProperty
        @Schema(description = "Connection id", example = "urn:topic-has-subtopics:345")
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

        TopicSubtopicIndexDocument() {
        }

        TopicSubtopicIndexDocument(NodeConnection nodeConnection) {
            id = nodeConnection.getPublicId();
            nodeConnection.getParent().ifPresent(topic -> topicid = topic.getPublicId());
            nodeConnection.getChild().ifPresent(subtopic -> subtopicid = subtopic.getPublicId());
            relevanceId = nodeConnection.getRelevance().map(Relevance::getPublicId).orElse(null);
            primary = true;
            rank = nodeConnection.getRank();
        }
    }
}
