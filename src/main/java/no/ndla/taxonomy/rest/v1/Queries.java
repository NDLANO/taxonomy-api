/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.dtos.EntityWithPathDTO;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = { "/v1/queries" })
public class Queries {
    private final Topics topicController;
    private final Resources resourceController;

    public Queries(Topics topicController, Resources resourceController) {
        this.topicController = topicController;
        this.resourceController = resourceController;
    }

    @GetMapping("/resources")
    @ApiOperation(value = "Gets a list of resources matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/resources?contentURI= instead")
    public List<ResourceDTO> queryResources(@RequestParam("contentURI") Optional<URI> contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "") Optional<String> language,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "key") Optional<String> key,
            @ApiParam(value = "Fitler by key and value") @RequestParam(value = "value") Optional<String> value,
            @ApiParam(value = "Filter by visible") @RequestParam(value = "isVisible") Optional<Boolean> isVisible) {
        return resourceController.getAll(language, contentURI, key, value, isVisible);
    }

    @GetMapping("/topics")
    @ApiOperation(value = "Gets a list of topics matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/topics?contentURI= instead")
    public List<EntityWithPathDTO> queryTopics(@RequestParam("contentURI") URI contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "") Optional<String> language,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "key") Optional<String> key,
            @ApiParam(value = "Fitler by key and value") @RequestParam(value = "value") Optional<String> value,
            @ApiParam(value = "Filter by visible") @RequestParam(value = "isVisible") Optional<Boolean> isVisible) {
        return topicController.getAll(language, Optional.of(contentURI), key, value, isVisible);
    }
}
