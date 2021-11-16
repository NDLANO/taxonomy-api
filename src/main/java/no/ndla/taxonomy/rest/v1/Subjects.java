/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.SubjectCommand;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RestController
@Transactional
@RequestMapping(path = { "/v1/subjects" })
public class Subjects extends CrudControllerWithMetadata<Node> {
    private final TreeSorter topicTreeSorter;
    private final RecursiveNodeTreeService recursiveNodeTreeService;
    private final ResourceService resourceService;
    private final NodeService nodeService;
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;

    public Subjects(TreeSorter treeSorter, CachedUrlUpdaterService cachedUrlUpdaterService,
            RecursiveNodeTreeService recursiveNodeTreeService, ResourceService resourceService,
            MetadataApiService metadataApiService, MetadataUpdateService metadataUpdateService, NodeService nodeService,
            NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository) {
        super(nodeRepository, cachedUrlUpdaterService, metadataApiService, metadataUpdateService);

        this.topicTreeSorter = treeSorter;
        this.recursiveNodeTreeService = recursiveNodeTreeService;
        this.resourceService = resourceService;
        this.nodeService = nodeService;
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
    }

    @GetMapping
    @ApiOperation("Gets all subjects")
    @InjectMetadata
    public List<EntityWithPathDTO> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "key", required = false) String key,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "value", required = false) String value) {
        if (key != null) {
            return nodeService.getNodes(language, NodeType.SUBJECT, null, new MetadataKeyValueQuery(key, value));
        }
        return nodeService.getNodes(language, NodeType.SUBJECT, null, false);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single subject", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    @InjectMetadata
    public EntityWithPathDTO get(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return nodeRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id)
                .map(subject -> new NodeDTO(subject, language))
                .orElseThrow(() -> new NotFoundHttpResponseException("Subject not found"));
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a subject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
            @ApiParam(name = "subject", value = "The updated subject. Fields not included will be set to null.") @RequestBody SubjectCommand command) {
        doPut(id, command);
    }

    @PostMapping
    @ApiOperation(value = "Creates a new subject")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "subject", value = "The new subject") @RequestBody SubjectCommand command) {
        final var subject = new Node(NodeType.SUBJECT);
        return doPost(subject, command);
    }

    @GetMapping("/{id}/topics")
    @ApiOperation(value = "Gets all children associated with a subject", notes = "This resource is read-only. To update the relationship between nodes, use the resource /subject-topics.")
    @InjectMetadata
    public List<EntityWithPathChildDTO> getChildren(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language,
            @ApiParam("If true, subtopics are fetched recursively") @RequestParam(value = "recursive", required = false, defaultValue = "false") boolean recursive,
            @Deprecated @ApiParam(value = "Select by filter id(s). If not specified, all topics will be returned."
                    + "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true) @RequestParam(value = "filter", required = false, defaultValue = "") Set<URI> filterIds,
            @ApiParam(value = "Select by relevance. If not specified, all nodes will be returned.") @RequestParam(value = "relevance", required = false, defaultValue = "") URI relevance) {
        final var subject = nodeRepository.findFirstByPublicId(id)
                .orElseThrow(() -> new NotFoundException("Subject", id));

        final List<Integer> childrenIds;

        if (recursive) {
            childrenIds = recursiveNodeTreeService.getRecursiveNodes(subject).stream()
                    .map(RecursiveNodeTreeService.TreeElement::getId).collect(Collectors.toList());
        } else {
            childrenIds = subject.getChildConnections().stream().map(EntityWithPathConnection::getConnectedChild)
                    .filter(Optional::isPresent).map(Optional::get).map(EntityWithPath::getId)
                    .collect(Collectors.toList());
        }

        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        final var children = nodeConnectionRepository
                .findAllByChildIdIncludeTranslationsAndCachedUrlsAndFilters(childrenIds);

        final var returnList = new ArrayList<EntityWithPathChildDTO>();

        // Filtering

        final var filteredConnections = children.stream()
                .filter(nodeConnection -> nodeConnection.getChild().isPresent())
                .filter(nodeConnection -> searchForRelevance(nodeConnection, relevanceArgument, children))
                .collect(Collectors.toList());

        // Wrapping with metadata from API if asked for

        filteredConnections.stream().map(nodeConnection -> createChildDTO(subject, nodeConnection, language))
                .forEach(returnList::add);

        // Remove duplicates from the list
        // List is sorted by parent, so we assume that any subtree that has a duplicate parent also
        // are repeated with the same subtree
        // on the duplicates, so starting to remove duplicates should not leave any parent-less

        // (Don't know how much it makes sense to sort the list by parent and rank when duplicates
        // are removed, but old code did)
        return topicTreeSorter.sortList(returnList).stream().distinct().collect(Collectors.toList());
    }

    private EntityWithPathChildDTO createChildDTO(Node subject, NodeConnection connection, String language) {
        return new NodeChildDTO(subject, connection, language);
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

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        nodeService.delete(id);
    }

    @GetMapping("/{subjectId}/resources")
    @ApiOperation(value = "Gets all resources for a subject. Searches recursively in all children of this node."
            + "The ordering of resources will be based on the rank of resources relative to the node they belong to.", tags = {
                    "subjects" })
    public List<ResourceWithNodeConnectionDTO> getResources(@PathVariable("subjectId") URI subjectId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language,
            @ApiParam(value = "Filter by resource type id(s). If not specified, resources of all types will be returned."
                    + "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true) @RequestParam(value = "type", required = false, defaultValue = "") URI[] resourceTypeIds,
            @Deprecated @ApiParam(value = "Select by filter id(s). If not specified, all resources will be returned."
                    + "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true) @RequestParam(value = "filter", required = false, defaultValue = "") URI[] filterIds,
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.") @RequestParam(value = "relevance", required = false, defaultValue = "") URI relevance) {
        final Set<URI> resourceTypeIdSet = resourceTypeIds != null ? Set.of(resourceTypeIds) : Set.of();

        // If null is sent to query it will be ignored, otherwise it will filter by relevance
        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        return resourceService.getResourcesByNodeId(subjectId, resourceTypeIdSet, relevanceArgument, language, true);
    }
}
