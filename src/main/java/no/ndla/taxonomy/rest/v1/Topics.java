/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.commands.TopicCommand;
import no.ndla.taxonomy.service.ContextUpdaterService;
import no.ndla.taxonomy.service.MetadataFilters;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.dtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Deprecated
@RequestMapping(path = { "/v1/topics" })
public class Topics extends CrudControllerWithMetadata<Node> {
    private final NodeRepository nodeRepository;
    private final NodeService nodeService;

    public Topics(NodeRepository nodeRepository, NodeService nodeService,
            ContextUpdaterService cachedUrlUpdaterService) {
        super(nodeRepository, cachedUrlUpdaterService);

        this.nodeRepository = nodeRepository;
        this.nodeService = nodeService;
    }

    @GetMapping
    @Operation(summary = "Gets all topics")
    @Transactional(readOnly = true)
    public List<NodeDTO> getAllTopics(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language,
            @Parameter(description = "Filter by contentUri") @RequestParam(value = "contentURI", required = false) Optional<URI> contentUri,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "key", required = false) Optional<String> key,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "value", required = false) Optional<String> value,
            @Parameter(description = "Filter by visible") @RequestParam(value = "isVisible", required = false) Optional<Boolean> isVisible) {

        MetadataFilters metadataFilters = new MetadataFilters(key, value, isVisible);
        return nodeService.getNodesByType(Optional.of(List.of(NodeType.TOPIC)), language, contentUri, Optional.empty(),
                Optional.empty(), metadataFilters);
    }

    @GetMapping("/search")
    @Operation(summary = "Search all topics")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> searchTopics(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language,
            @Parameter(description = "How many results to return per page") @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @Parameter(description = "Which page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Query to search names") @RequestParam(value = "query", required = false) Optional<String> query,
            @Parameter(description = "Ids to fetch for query") @RequestParam(value = "ids", required = false) Optional<List<String>> ids

    ) {
        return nodeService.searchByNodeType(query, ids, language, pageSize, page, Optional.of(NodeType.TOPIC));
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all topics paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> getTopicsPage(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false) Optional<String> language,
            @Parameter(name = "page", description = "The page to fetch") Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch") Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeRepository.findIdsByTypePaginated(PageRequest.of(page.get() - 1, pageSize.get()), NodeType.TOPIC);
        var results = nodeRepository.findByIds(ids.getContent());
        var contents = results.stream()
                .map(node -> new NodeDTO(Optional.empty(), node, language.orElse("nb"), Optional.empty()))
                .collect(Collectors.toList());
        return new SearchResultDTO<>(ids.getTotalElements(), page.get(), pageSize.get(), contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a single topic")
    @Transactional(readOnly = true)
    public NodeDTO getTopic(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language) {
        return nodeService.getNode(id, language);
    }

    @PostMapping
    @Operation(summary = "Creates a new topic", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createTopic(
            @Parameter(name = "connection", description = "The new topic") @RequestBody @Schema(name = "TopicPOST") TopicCommand command) {
        return createEntity(new Node(NodeType.TOPIC), command);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a single topic", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateTopic(@PathVariable("id") URI id,
            @Parameter(name = "topic", description = "The updated topic. Fields not included will be set to null.") @RequestBody @Schema(name = "VersionPUT") TopicCommand command) {
        updateEntity(id, command);
    }

    @GetMapping("/{id}/topics")
    @Operation(summary = "Gets all subtopics for this topic")
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getTopicSubTopics(@Parameter(name = "id", required = true) @PathVariable("id") URI id,
            @Parameter(description = "Select filters by subject id if filter list is empty. Used as alternative to specify filters.") @RequestParam(value = "subject", required = false, defaultValue = "") URI subjectId,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language) {
        return nodeService.getFilteredChildConnections(id, language.orElse(Constants.DefaultLanguage));
    }

    @GetMapping("/{id}/connections")
    @Operation(summary = "Gets all subjects and subtopics this topic is connected to")
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getAllTopicConnections(@PathVariable("id") URI id) {
        return nodeService.getAllConnections(id);
    }

    @DeleteMapping("/{id}")
    @Operation(description = "Deletes a single entity by id", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        nodeService.delete(id);
    }

    @GetMapping("/{id}/resources")
    @Operation(summary = "Gets all resources for the given topic", tags = { "topics" })
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getTopicResources(
            @Parameter(name = "id", required = true) @PathVariable("id") URI topicId,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language,
            @Parameter(description = "If true, resources from subtopics are fetched recursively") @RequestParam(value = "recursive", required = false, defaultValue = "false") boolean recursive,
            @Parameter(description = "Select by resource type id(s). If not specified, resources of all types will be returned."
                    + "Multiple ids may be separated with comma or the parameter may be repeated for each id.") @RequestParam(value = "type", required = false) URI[] resourceTypeIds,
            @Parameter(description = "Select by relevance. If not specified, all resources will be returned.") @RequestParam(value = "relevance", required = false) URI relevance) {
        final Set<URI> resourceTypeIdSet;

        if (resourceTypeIds == null) {
            resourceTypeIdSet = Set.of();
        } else {
            resourceTypeIdSet = new HashSet<>(Arrays.asList(resourceTypeIds));
        }

        return nodeService.getResourcesByNodeId(topicId, resourceTypeIdSet, relevance, language, recursive);
    }

    @PutMapping("/{id}/makeResourcesPrimary")
    @Operation(summary = "Makes all connected resources primary", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @Transactional
    public ResponseEntity<Boolean> makeResourcesPrimary(
            @Parameter(name = "id", required = true) @PathVariable("id") URI nodeId,
            @Parameter(description = "If true, children are fetched recursively") @RequestParam(value = "recursive", required = false, defaultValue = "false") boolean recursive) {
        return ResponseEntity.of(Optional.of(nodeService.makeAllResourcesPrimary(nodeId, recursive)));
    }
}
