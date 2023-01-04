/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.MetadataFilters;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.ResourceService;
import no.ndla.taxonomy.service.dtos.EntityWithPathDTO;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = { "/v1/queries" })
public class Queries {
    private final Topics topicController;
    private final Resources resourceController;
    private final NodeService nodeService;
    private final ResourceService resourceService;

    public Queries(Topics topicController, Resources resourceController, NodeService nodeService,
            ResourceService resourceService) {
        this.topicController = topicController;
        this.resourceController = resourceController;
        this.nodeService = nodeService;
        this.resourceService = resourceService;
    }

    @GetMapping("/{contentURI}")
    @ApiOperation(value = "Gets all related data for a given contentURI")
    // TODO: Make a shared type somewhere, don't use `Object` :^)
    // TODO: Please
    // TODO: Dont
    // TODO: Be
    // TODO: This
    // TODO: Stupid
    public List<Object> queryAll(@PathVariable("contentURI") URI contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "", required = false) Optional<String> language) {
        // TODO: Make a shared type somewhere, don't use `Object` :^)
        var list = new ArrayList<Object>();
        MetadataFilters metadataFilters = MetadataFilters.empty();
        var nodes = nodeService.getNodes(language, Optional.empty(), Optional.of(contentURI), Optional.empty(),
                metadataFilters);

        var resources = resourceService.getResources(language, Optional.of(contentURI), metadataFilters);

        list.addAll(nodes);
        list.addAll(resources);

        return list;
    }

    @GetMapping("/resources")
    @ApiOperation(value = "Gets a list of resources matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/resources?contentURI= instead")
    public List<EntityWithPathDTO> queryResources(@RequestParam("contentURI") Optional<URI> contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "", required = false) Optional<String> language,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "key", required = false) Optional<String> key,
            @ApiParam(value = "Fitler by key and value") @RequestParam(value = "value", required = false) Optional<String> value,
            @ApiParam(value = "Filter by visible") @RequestParam(value = "isVisible", required = false) Optional<Boolean> isVisible) {
        return resourceController.getAll(language, contentURI, key, value, isVisible);
    }

    @GetMapping("/topics")
    @ApiOperation(value = "Gets a list of topics matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/topics?contentURI= instead")
    public List<EntityWithPathDTO> queryTopics(@RequestParam("contentURI") URI contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "", required = false) Optional<String> language,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "key", required = false) Optional<String> key,
            @ApiParam(value = "Fitler by key and value") @RequestParam(value = "value", required = false) Optional<String> value,
            @ApiParam(value = "Filter by visible") @RequestParam(value = "isVisible", required = false) Optional<Boolean> isVisible) {
        return topicController.getAll(language, Optional.of(contentURI), key, value, isVisible);
    }
}
