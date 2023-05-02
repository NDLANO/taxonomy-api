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
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.commands.SubjectCommand;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/subjects" })
public class Subjects extends CrudControllerWithMetadata<Node> {
    private final TreeSorter topicTreeSorter;
    private final RecursiveNodeTreeService recursiveNodeTreeService;
    private final NodeService nodeService;
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;

    public Subjects(TreeSorter treeSorter, ContextUpdaterService cachedUrlUpdaterService,
            RecursiveNodeTreeService recursiveNodeTreeService, NodeService nodeService, NodeRepository nodeRepository,
            NodeConnectionRepository nodeConnectionRepository) {
        super(nodeRepository, cachedUrlUpdaterService);

        this.topicTreeSorter = treeSorter;
        this.recursiveNodeTreeService = recursiveNodeTreeService;
        this.nodeService = nodeService;
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Gets all subjects")
    @Transactional(readOnly = true)
    public List<NodeDTO> getAllSubjects(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "key", required = false) Optional<String> key,
            @Parameter(description = "Fitler by key and value") @RequestParam(value = "value", required = false) Optional<String> value,
            @Parameter(description = "Filter by visible") @RequestParam(value = "isVisible", required = false) Optional<Boolean> isVisible) {
        MetadataFilters metadataFilters = new MetadataFilters(key, value, isVisible);
        return nodeService.getNodesByType(Optional.of(List.of(NodeType.SUBJECT)), language, Optional.empty(),
                Optional.empty(), Optional.empty(), metadataFilters, Optional.of(false));
    }

    @Deprecated
    @GetMapping("/search")
    @Operation(summary = "Search all subjects")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> searchSubjects(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language,
            @Parameter(description = "How many results to return per page") @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @Parameter(description = "Which page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Query to search names") @RequestParam(value = "query", required = false) Optional<String> query,
            @Parameter(description = "Ids to fetch for query") @RequestParam(value = "ids", required = false) Optional<List<String>> ids

    ) {
        return nodeService.searchByNodeType(query, ids, language, Optional.of(false), pageSize, page,
                Optional.of(NodeType.SUBJECT));
    }

    @Deprecated
    @GetMapping("/page")
    @Operation(summary = "Gets all connections between node and children paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> getSubjectPage(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false) Optional<String> language,
            @Parameter(name = "page", description = "The page to fetch") Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch") Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = nodeRepository.findIdsByTypePaginated(PageRequest.of(page.get() - 1, pageSize.get()),
                NodeType.SUBJECT);
        var results = nodeRepository.findByIds(ids.getContent());
        var contents = results.stream().map(node -> new NodeDTO(Optional.empty(), node, language.orElse("nb"),
                Optional.empty(), Optional.of(false))).collect(Collectors.toList());
        return new SearchResultDTO<>(ids.getTotalElements(), page.get(), pageSize.get(), contents);
    }

    @Deprecated
    @GetMapping("/{id}")
    @Operation(summary = "Gets a single subject", description = "Default language will be returned if desired language not found or if parameter is omitted.")
    @Transactional(readOnly = true)
    public NodeDTO getSubject(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language) {
        return nodeService.getNode(id, language, Optional.of(false));
    }

    @Deprecated
    @PutMapping("/{id}")
    @Operation(summary = "Updates a subject", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateSubject(@PathVariable("id") URI id,
            @Parameter(name = "subject", description = "The updated subject. Fields not included will be set to null.") @RequestBody @Schema(name = "SubjectPOST") SubjectCommand command) {
        updateEntity(id, command);
    }

    @Deprecated
    @PostMapping
    @Operation(summary = "Creates a new subject", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createSubject(
            @Parameter(name = "subject", description = "The new subject") @RequestBody @Schema(name = "SubjectPUT") SubjectCommand command) {
        final var subject = new Node(NodeType.SUBJECT);
        return createEntity(subject, command);
    }

    @Deprecated
    @GetMapping("/{id}/topics")
    @Operation(summary = "Gets all children associated with a subject", description = "This resource is read-only. To update the relationship between nodes, use the resource /subject-topics.")
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getSubjectChildren(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language,
            @Parameter(description = "If true, subtopics are fetched recursively") @RequestParam(value = "recursive", required = false, defaultValue = "false") boolean recursive,
            @Parameter(description = "Select by relevance. If not specified, all nodes will be returned.") @RequestParam(value = "relevance", required = false, defaultValue = "") URI relevance) {
        final var subject = nodeRepository.findFirstByPublicId(id)
                .orElseThrow(() -> new NotFoundException("Subject", id));

        final List<URI> childrenIds;

        if (recursive) {
            childrenIds = recursiveNodeTreeService.getRecursiveNodes(subject).stream()
                    .map(RecursiveNodeTreeService.TreeElement::getId).collect(Collectors.toList());
        } else {
            childrenIds = subject.getChildConnections().stream().map(NodeConnection::getChild)
                    .filter(Optional::isPresent).map(Optional::get).map(Node::getPublicId).collect(Collectors.toList());
        }

        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        final var children = nodeConnectionRepository
                .findAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(childrenIds);

        final var returnList = new ArrayList<NodeChildDTO>();

        final var connections = children.stream().filter(nodeConnection -> {
            var child = nodeConnection.getChild();
            var relevanceFilter = searchForRelevance(nodeConnection, relevanceArgument, children);
            return child.isPresent() && child.get().getNodeType() == NodeType.TOPIC && relevanceFilter;
        }).toList();

        connections.stream().map(nodeConnection -> new NodeChildDTO(Optional.of(subject), nodeConnection,
                language.orElse(Constants.DefaultLanguage), Optional.of(false))).forEach(returnList::add);

        var filtered = returnList.stream().filter(childDTO -> childrenIds.contains(childDTO.getParentId())
                || subject.getPublicId().equals(childDTO.getParentId())).toList();

        // Remove duplicates from the list
        // List is sorted by parent, so we assume that any subtree that has a duplicate parent also
        // are repeated with the same subtree
        // on the duplicates, so starting to remove duplicates should not leave any parent-less

        // (Don't know how much it makes sense to sort the list by parent and rank when duplicates
        // are removed, but old code did)
        return topicTreeSorter.sortList(filtered).stream().distinct().collect(Collectors.toList());
    }

    private boolean searchForRelevance(NodeConnection connection, URI relevancePublicId,
            Collection<NodeConnection> children) {
        if (relevancePublicId == null) {
            return true;
        }

        final var foundFilter = new AtomicBoolean(false);
        Node node = connection.getChild().orElse(null);

        if (node != null) {
            if (connection.getRelevance().isPresent()) {
                foundFilter.set(connection.getRelevance().get().getPublicId().equals(relevancePublicId));
            }

            children.stream().filter(st -> st.getParent().isPresent()).forEach(nodeConnection -> {
                if (nodeConnection.getParent().get().getId().equals(node.getId())) {
                    if (searchForRelevance(nodeConnection, relevancePublicId, children)) {
                        foundFilter.set(true);
                    }
                }
            });
        }

        return foundFilter.get();
    }

    @Deprecated
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes a single entity by id", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        nodeService.delete(id);
    }

    @Deprecated
    @GetMapping("/{subjectId}/resources")
    @Operation(summary = "Gets all resources for a subject. Searches recursively in all children of this node."
            + "The ordering of resources will be based on the rank of resources relative to the node they belong to.", tags = {
                    "subjects" })
    @Transactional(readOnly = true)
    public List<NodeChildDTO> getSubjectResources(@PathVariable("subjectId") URI subjectId,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage) Optional<String> language,
            @Parameter(description = "Filter by resource type id(s). If not specified, resources of all types will be returned."
                    + "Multiple ids may be separated with comma or the parameter may be repeated for each id.") @RequestParam(value = "type", required = false, defaultValue = "") URI[] resourceTypeIds,
            @Parameter(description = "Select by relevance. If not specified, all resources will be returned.") @RequestParam(value = "relevance", required = false, defaultValue = "") URI relevance) {
        final Set<URI> resourceTypeIdSet = resourceTypeIds != null ? Set.of(resourceTypeIds) : Set.of();

        // If null is sent to query it will be ignored, otherwise it will filter by relevance
        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        return nodeService.getResourcesByNodeId(subjectId, resourceTypeIdSet, relevanceArgument, language, true,
                Optional.of(false));
    }
}
