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
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.domain.exceptions.PrimaryParentRequiredException;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.rest.v1.dtos.nodes.NodeConnectionPage;
import no.ndla.taxonomy.rest.v1.dtos.nodes.ParentChildIndexDocument;
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
@RequestMapping(path = {"/v1/topic-resources"})
@Transactional
public class TopicResources {

    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityConnectionService connectionService;
    private final RelevanceRepository relevanceRepository;

    public TopicResources(
            NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository,
            EntityConnectionService connectionService,
            RelevanceRepository relevanceRepository
    ) {
        this.nodeRepository = nodeRepository;
        this.connectionService = connectionService;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.relevanceRepository = relevanceRepository;
    }

    @GetMapping
    @ApiOperation(value = "Gets all connections between topics and resources")
    public List<ParentChildIndexDocument> index() {
        return nodeConnectionRepository
                .findAllByChildNodeType(NodeType.RESOURCE)
                .stream().map(ParentChildIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/page")
    @ApiOperation(value = "Gets all connections between topic and resources paginated")
    public NodeConnectionPage allPaginated(
            @ApiParam(name = "page", value = "The page to fetch", required = true) Optional<Integer> page,
            @ApiParam(name = "pageSize", value = "Size of page to fetch", required = true) Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page.get() - 1, pageSize.get());
        var ids = nodeConnectionRepository.findIdsPaginatedByChildNodeType(pageRequest, NodeType.RESOURCE);
        var results = nodeConnectionRepository.findByIds(ids.getContent());
        var contents = results.stream().map(ParentChildIndexDocument::new).collect(Collectors.toList());
        return new NodeConnectionPage(ids.getTotalElements(), contents);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a specific connection between a topic and a resource")
    public ParentChildIndexDocument get(@PathVariable("id") URI id) {
        var resourceConnection = nodeConnectionRepository.getByPublicId(id);
        return new ParentChildIndexDocument(resourceConnection);
    }

    @PostMapping
    @ApiOperation(value = "Adds a resource to a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "new topic/resource connection ") @RequestBody AddResourceToTopicCommand command) {

        Node topic = nodeRepository.getByPublicId(command.topicid);
        Node resource = nodeRepository.getByPublicId(command.resourceId);
        Relevance relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId)
                : null;

        var primary = Optional.of(command.primary);
        var rank = command.rank == 0 ? null : command.rank;

        final NodeConnection topicResource;
        topicResource = connectionService.connectParentChild(topic, resource, relevance, rank, primary);

        URI location = URI.create("/node-child/" + topicResource.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Removes a resource from a topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectAllParents(id);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a connection between a topic and a resource", notes = "Use to update which topic is primary to the resource or to change sorting order.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
                    @ApiParam(name = "connection", value = "Updated topic/resource connection") @RequestBody UpdateTopicResourceCommand command) {
        var topicResource = nodeConnectionRepository.getByPublicId(id);
        var relevance = command.relevanceId != null ? relevanceRepository.getByPublicId(command.relevanceId) : null;

        if (topicResource.isPrimary().orElse(false) && !command.primary) {
            throw new PrimaryParentRequiredException();
        }
        var rank = command.rank > 0 ? command.rank : null;
        var primary = Optional.of(command.primary);

        connectionService.updateParentChild(topicResource, relevance, rank, primary);
    }

    public static class AddResourceToTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:345")
        public URI resourceId;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary = true;

        @JsonProperty
        @ApiModelProperty(value = "Order in which resource is sorted for the topic", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateTopicResourceCommand {
        @JsonProperty
        @ApiModelProperty(value = "Topic resource connection id", example = "urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the resource will be sorted for this topic.", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class TopicResourcePage {
        @JsonProperty
        @ApiModelProperty(value = "Total number of elements")
        public long totalCount;

        @JsonProperty
        @ApiModelProperty(value = "Page containing results")
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
        @ApiModelProperty(value = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Resource id", example = "urn:resource:345")
        URI resourceId;

        @JsonProperty
        @ApiModelProperty(value = "Topic resource connection id", example = "urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the resource is sorted for the topic", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

    }
}
