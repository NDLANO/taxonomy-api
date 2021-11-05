/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.rest.v1.commands.ResourceCommand;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import no.ndla.taxonomy.service.dtos.ResourceTypeWithConnectionDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithParentTopicsDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithTopicConnectionDTO;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/v1/resources")
public class Resources extends CrudControllerWithMetadata<Resource> {
    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private final ResourceService resourceService;

    public Resources(ResourceRepository resourceRepository,
            ResourceResourceTypeRepository resourceResourceTypeRepository, ResourceService resourceService,
            CachedUrlUpdaterService cachedUrlUpdaterService, MetadataApiService metadataApiService,
            MetadataUpdateService metadataUpdateService) {
        super(resourceRepository, cachedUrlUpdaterService, metadataApiService, metadataUpdateService);

        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
        this.repository = resourceRepository;
        this.resourceService = resourceService;
    }

    @Override
    protected String getLocation() {
        return "/v1/resources";
    }

    @GetMapping
    @ApiOperation(value = "Lists all resources")
    @Transactional(readOnly = true)
    public List<ResourceDTO> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language,
            @RequestParam(value = "contentURI", required = false) @ApiParam(value = "Filter by contentUri") URI contentUriFilter,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "key", required = false) String key,
            @ApiParam(value = "Fitler by key and value") @RequestParam(value = "value", required = false) String value) {
        if (contentUriFilter != null && contentUriFilter.toString().equals("")) {
            contentUriFilter = null;
        }

        if (key != null) {
            return resourceService.getResources(language, contentUriFilter, new MetadataKeyValueQuery(key, value));
        }
        return resourceService.getResources(language, contentUriFilter);
    }

    @GetMapping("{id}")
    @ApiOperation(value = "Gets a single resource")
    public ResourceDTO get(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {

        return resourceService.getResourceByPublicId(id, language);
    }

    @PutMapping("{id}")
    @ApiOperation(value = "Updates a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(@PathVariable("id") URI id,
            @ApiParam(name = "resource", value = "the updated resource. Fields not included will be set to null.") @RequestBody ResourceCommand command) {
        doPut(id, command);
    }

    @PostMapping
    @ApiOperation(value = "Adds a new resource")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(
            @ApiParam(name = "resource", value = "the new resource") @RequestBody ResourceCommand command) {
        return doPost(new Resource(), command);
    }

    @GetMapping("{id}/resource-types")
    @ApiOperation(value = "Gets all resource types associated with this resource")
    @Transactional(readOnly = true)
    public List<ResourceTypeWithConnectionDTO> getResourceTypes(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {

        return resourceResourceTypeRepository
                .findAllByResourcePublicIdIncludingResourceAndResourceTypeAndResourceTypeParent(id).stream()
                .map(resourceResourceType -> new ResourceTypeWithConnectionDTO(resourceResourceType, language))
                .collect(Collectors.toList());
    }

    @GetMapping("{id}/full")
    @ApiOperation(value = "Gets all parent topics, all filters and resourceTypes for this resource")
    @Transactional(readOnly = true)
    public ResourceWithParentTopicsDTO getResourceFull(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return resourceService.getResourceWithParentTopicsByPublicId(id, language);
    }

    @DeleteMapping("{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        resourceService.delete(id);
    }
}
