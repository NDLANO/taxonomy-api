/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.TopicCommand;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = { "/v1/topics" })
public class Topics extends CrudControllerWithMetadata<Node> {
    private final NodeRepository nodeRepository;
    private final NodeService nodeService;
    private final ResourceService resourceService;

    public Topics(NodeRepository nodeRepository, NodeService nodeService,
            CachedUrlUpdaterService cachedUrlUpdaterService, ResourceService resourceService,
            MetadataApiService metadataApiService, MetadataUpdateService metadataUpdateService) {
        super(nodeRepository, cachedUrlUpdaterService, metadataApiService, metadataUpdateService);

        this.nodeRepository = nodeRepository;
        this.nodeService = nodeService;
        this.resourceService = resourceService;
    }

    @GetMapping
    @ApiOperation("Gets all topics")
    public List<EntityWithPathDTO> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language,

            @ApiParam(value = "Filter by contentUri") @RequestParam(value = "contentURI", required = false) URI contentUriFilter,

            @ApiParam(value = "Filter by key and value") @RequestParam(value = "key", required = false) String key,

            @ApiParam(value = "Filter by key and value") @RequestParam(value = "value", required = false) String value) {

        if (contentUriFilter != null && contentUriFilter.toString().equals("")) {
            contentUriFilter = null;
        }
        if (key != null) {
            return nodeService.getNodes(language, NodeType.TOPIC, contentUriFilter,
                    new MetadataKeyValueQuery(key, value));
        }
        return nodeService.getNodes(language, NodeType.TOPIC, contentUriFilter, false);
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    @Transactional
    public EntityWithPathDTO get(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return new NodeDTO(nodeRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id)
                .orElseThrow(() -> new NotFoundHttpResponseException("Topic was not found")), language);
    }

    @PostMapping
    @ApiOperation(value = "Creates a new topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new topic") @RequestBody TopicCommand command) {
        return doPost(new Node(NodeType.TOPIC), command);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a single topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(@PathVariable("id") URI id,
            @ApiParam(name = "topic", value = "The updated topic. Fields not included will be set to null.") @RequestBody TopicCommand command) {
        doPut(id, command);
    }

    @GetMapping("/{id}/resource-types")
    @ApiOperation(value = "Gets all resource types associated with this topic. No longer needed since o topics in database have resource-type")
    @Deprecated(forRemoval = true)
    @Transactional
    public List<ResourceTypeDTO> getResourceTypes(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return List.of();
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters associated with this topic")
    @Deprecated(forRemoval = true)
    @Transactional
    public List<Object> getFilters(@ApiParam(value = "id", required = true) @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return List.of();
    }

    @GetMapping("/{id}/topics")
    @ApiOperation(value = "Gets all subtopics for this topic")
    public List<TopicChildDTO> getSubTopics(@ApiParam(value = "id", required = true) @PathVariable("id") URI id,
            @ApiParam(value = "Select filters by subject id if filter list is empty. Used as alternative to specify filters.") @RequestParam(value = "subject", required = false, defaultValue = "") URI subjectId,
            @Deprecated(forRemoval = true) @ApiParam(value = "Select by filter id(s). If not specified, all subtopics connected to this topic will be returned."
                    + "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true) @RequestParam(value = "filter", required = false, defaultValue = "") URI[] filterIds,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return nodeService.getFilteredChildConnections(id, language);
    }

    @GetMapping("/{id}/connections")
    @ApiOperation(value = "Gets all subjects and subtopics this topic is connected to")
    public List<ConnectionIndexDTO> getAllConnections(@PathVariable("id") URI id) {
        return nodeService.getAllConnections(id);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        nodeService.delete(id);
    }

    @GetMapping("/{id}/resources")
    @ApiOperation(value = "Gets all resources for the given topic", tags = { "topics" })
    public List<ResourceWithNodeConnectionDTO> getResources(
            @ApiParam(value = "id", required = true) @PathVariable("id") URI topicId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false) String language,
            @ApiParam("If true, resources from subtopics are fetched recursively") @RequestParam(value = "recursive", required = false, defaultValue = "false") boolean recursive,
            @ApiParam(value = "Select by resource type id(s). If not specified, resources of all types will be returned."
                    + "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true) @RequestParam(value = "type", required = false) URI[] resourceTypeIds,
            @Deprecated @ApiParam(value = "Select filters by subject id if filter list is empty. Used as alternative to specify filters.") @RequestParam(value = "subject", required = false) URI subjectId,
            @Deprecated @ApiParam(value = "Select by filter id(s). If not specified, all resources will be returned."
                    + "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true) @RequestParam(value = "filter", required = false) URI[] filterIds,
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.") @RequestParam(value = "relevance", required = false) URI relevance) {
        final Set<URI> resourceTypeIdSet;

        if (resourceTypeIds == null) {
            resourceTypeIdSet = Set.of();
        } else {
            resourceTypeIdSet = new HashSet<>(Arrays.asList(resourceTypeIds));
        }

        return resourceService.getResourcesByNodeId(topicId, resourceTypeIdSet, relevance, language, recursive);
    }
}
