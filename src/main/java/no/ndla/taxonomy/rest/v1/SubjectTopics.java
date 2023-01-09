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
import no.ndla.taxonomy.repositories.*;
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
@RequestMapping(path = { "/v1/subject-topics" })
@Transactional
public class SubjectTopics {
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;

    public SubjectTopics(NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService, RelevanceRepository relevanceRepository) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.connectionService = connectionService;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @ApiOperation("Gets all connections between subjects and topics")
    public List<SubjectTopicIndexDocument> index() {
        return nodeConnectionRepository.findAllIncludingParentAndChild().stream().map(SubjectTopicIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @ApiOperation("Gets all connections between subjects and topics paginated")
    public SubjectTopicPage allPaginated(@ApiParam(name = "page", value = "The page to fetch") Optional<Integer> page,
            @ApiParam(name = "pageSize", value = "Size of page to fetch") Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeConnectionRepository.findIdsPaginated(PageRequest.of(page.get() - 1, pageSize.get()));
        var results = nodeConnectionRepository.findByIds(ids.getContent());
        var contents = results.stream().map(SubjectTopicIndexDocument::new).collect(Collectors.toList());
        return new SubjectTopicPage(ids.getTotalElements(), contents);
    }

    @GetMapping("/{id}")
    @ApiOperation("Get a specific connection between a subject and a topic")
    public SubjectTopicIndexDocument get(@PathVariable("id") URI id) {
        NodeConnection nodeConnection = nodeConnectionRepository.getByPublicId(id);
        return new SubjectTopicIndexDocument(nodeConnection);
    }

    @PostMapping
    @ApiOperation("Adds a new topic to a subject")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "command", value = "The subject and topic getting connected.") @RequestBody AddTopicToSubjectCommand command) {
        var subject = nodeRepository.getByPublicId(command.subjectid);
        var topic = nodeRepository.getByPublicId(command.topicid);
        var relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId) : null;
        var rank = command.rank == 0 ? null : command.rank;
        final var nodeConnection = connectionService.connectParentChild(subject, topic, relevance, rank,
                Optional.empty());
        var location = URI.create("/subject-topics/" + nodeConnection.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Removes a topic from a subject")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectParentChildConnection(nodeConnectionRepository.getByPublicId(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Updates a connection between subject and topic", notes = "Use to update which subject is primary to a topic or to change sorting order.")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @ApiParam(name = "connection", value = "updated subject/topic connection") @RequestBody UpdateSubjectTopicCommand command) {
        var nodeConnection = nodeConnectionRepository.getByPublicId(id);
        var relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId) : null;
        var rank = command.rank > 0 ? command.rank : null;

        connectionService.updateParentChild(nodeConnection, relevance, rank, Optional.empty());
    }

    public static class AddTopicToSubjectCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Subject id", example = "urn:subject:123")
        public URI subjectid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Backwards compatibility: Always true, ignored on insert/update.", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the topic should be sorted for the topic", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateSubjectTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "connection id", example = "urn:subject-topic:2")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "If true, set this subject as the primary subject for this topic", example = "true", notes = "This will replace any other primary subject for this topic. You must have one primary subject, so it is not allowed to set the currently primary subject to not be primary any more.")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the topic should be sorted for the subject", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class SubjectTopicPage {
        @JsonProperty
        @ApiModelProperty(value = "Total number of elements")
        public long totalCount;

        @JsonProperty
        @ApiModelProperty(value = "Page containing results")
        public List<SubjectTopicIndexDocument> results;

        SubjectTopicPage() {
        }

        SubjectTopicPage(long totalCount, List<SubjectTopicIndexDocument> results) {
            this.totalCount = totalCount;
            this.results = results;
        }
    }

    public static class SubjectTopicIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Subject id", example = "urn:subject:123")
        public URI subjectid;

        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Connection id", example = "urn:subject-has-topics:34")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "primary", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the topic is sorted under the subject", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        SubjectTopicIndexDocument() {
        }

        SubjectTopicIndexDocument(NodeConnection nodeConnection) {
            id = nodeConnection.getPublicId();

            subjectid = nodeConnection.getParent().map(Node::getPublicId).orElse(null);

            topicid = nodeConnection.getChild().map(Node::getPublicId).orElse(null);

            primary = true;
            rank = nodeConnection.getRank();
            relevanceId = nodeConnection.getRelevance().map(Relevance::getPublicId).orElse(null);
        }
    }
}
