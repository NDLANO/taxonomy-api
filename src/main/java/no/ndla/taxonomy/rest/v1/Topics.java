/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicRepository;
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
@RequestMapping(path = {"/v1/topics"})
public class Topics extends CrudControllerWithMetadata<Topic> {
    private final TopicRepository topicRepository;
    private final TopicService topicService;
    private final ResourceService resourceService;

    public Topics(
            TopicRepository topicRepository,
            TopicService topicService,
            CachedUrlUpdaterService cachedUrlUpdaterService,
            ResourceService resourceService,
            MetadataApiService metadataApiService,
            MetadataUpdateService metadataUpdateService) {
        super(topicRepository, cachedUrlUpdaterService, metadataApiService, metadataUpdateService);

        this.topicRepository = topicRepository;
        this.topicService = topicService;
        this.resourceService = resourceService;
    }

    @GetMapping
    @ApiOperation("Gets all topics")
    public List<TopicDTO> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @ApiParam(value = "Filter by contentUri")
                    @RequestParam(value = "contentURI", required = false)
                    URI contentUriFilter,
            @ApiParam(value = "Filter by key and value")
                    @RequestParam(value = "key", required = false)
                    String key,
            @ApiParam(value = "Fitler by key and value")
                    @RequestParam(value = "value", required = false)
                    String value) {

        if (contentUriFilter != null && contentUriFilter.toString().equals("")) {
            contentUriFilter = null;
        }
        if (key != null) {
            return topicService.getTopics(
                    language, contentUriFilter, new MetadataKeyValueQuery(key, value));
        }
        return topicService.getTopics(language, contentUriFilter);
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    @Transactional
    @InjectMetadata
    public TopicDTO get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        return new TopicDTO(
                topicRepository
                        .findFirstByPublicIdIncludingCachedUrlsAndTranslations(id)
                        .orElseThrow(
                                () -> new NotFoundHttpResponseException("Topic was not found")),
                language);
    }

    @PostMapping
    @ApiOperation(value = "Creates a new topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new topic") @RequestBody
                    TopicCommand command) {
        return doPost(new Topic(), command);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a single topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(
                            name = "topic",
                            value = "The updated topic. Fields not included will be set to null.")
                    @RequestBody
                    TopicCommand command) {
        doPut(id, command);
    }

    @GetMapping("/{id}/resource-types")
    @ApiOperation(
            value =
                    "Gets all resource types associated with this topic. No longer needed since o topics in database have resource-type")
    @Deprecated(forRemoval = true)
    @Transactional
    public List<ResourceTypeDTO> getResourceTypes(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        return List.of();
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters associated with this topic")
    @Transactional
    @InjectMetadata
    public List<Object> getFilters(
            @ApiParam(value = "id", required = true) @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        return List.of();
    }

    @GetMapping("/{id}/topics")
    @ApiOperation(value = "Gets all subtopics for this topic")
    public List<SubTopicIndexDTO> getSubTopics(
            @ApiParam(value = "id", required = true) @PathVariable("id") URI id,
            @RequestParam(value = "subject", required = false, defaultValue = "")
                    @ApiParam(
                            value =
                                    "Select filters by subject id if filter list is empty. Used as alternative to specify filters.")
                    URI subjectId,
            @RequestParam(value = "filter", required = false, defaultValue = "")
                    @ApiParam(
                            value =
                                    "Select by filter id(s). If not specified, all subtopics connected to this topic will be returned."
                                            + "Multiple ids may be separated with comma or the parameter may be repeated for each id.",
                            allowMultiple = true)
                    URI[] filterIds,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = "")
                    String language) {
        if (filterIds == null) {
            filterIds = new URI[0];
        }

        if (filterIds.length == 0) {
            return topicService.getFilteredSubtopicConnections(id, subjectId, language);
        }

        return List.of(); // Requested filtered, we don't have filters.
    }

    @GetMapping("/{id}/connections")
    @ApiOperation(value = "Gets all subjects and subtopics this topic is connected to")
    public List<ConnectionIndexDTO> getAllConnections(@PathVariable("id") URI id) {
        return topicService.getAllConnections(id);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        topicService.delete(id);
    }

    @GetMapping("/{id}/resources")
    @ApiOperation(
            value = "Gets all resources for the given topic",
            tags = {"topics"})
    public List<ResourceWithTopicConnectionDTO> getResources(
            @ApiParam(value = "id", required = true) @PathVariable("id") URI topicId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false)
                    String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
                    @ApiParam("If true, resources from subtopics are fetched recursively")
                    boolean recursive,
            @RequestParam(value = "type", required = false)
                    @ApiParam(
                            value =
                                    "Select by resource type id(s). If not specified, resources of all types will be returned."
                                            + "Multiple ids may be separated with comma or the parameter may be repeated for each id.",
                            allowMultiple = true)
                    URI[] resourceTypeIds,
            @RequestParam(value = "subject", required = false)
                    @ApiParam(
                            value =
                                    "Select filters by subject id if filter list is empty. Used as alternative to specify filters.")
                    URI subjectId,
            @RequestParam(value = "filter", required = false)
                    @ApiParam(
                            value =
                                    "Select by filter id(s). If not specified, all resources will be returned."
                                            + "Multiple ids may be separated with comma or the parameter may be repeated for each id.",
                            allowMultiple = true)
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false)
                    @ApiParam(
                            value =
                                    "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance) {
        final Set<URI> resourceTypeIdSet;
        final Set<URI> filterIdSet;

        if (resourceTypeIds == null) {
            resourceTypeIdSet = Set.of();
        } else {
            resourceTypeIdSet = new HashSet<>(Arrays.asList(resourceTypeIds));
        }

        if (filterIds == null) {
            filterIdSet = Set.of();
        } else {
            filterIdSet = new HashSet<>(Arrays.asList(filterIds));
        }

        if (filterIdSet.isEmpty()) {
            return resourceService.getResourcesByTopicId(
                    topicId, subjectId, resourceTypeIdSet, relevance, language, recursive);
        } else {
            return List.of(); // We don't have filters.
        }
    }
}
