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
import no.ndla.taxonomy.service.dtos.ResourceWithParentsDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/v1/resources")
public class Resources extends CrudControllerWithMetadata<Resource> {
    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private final ResourceService resourceService;

    public Resources(ResourceRepository resourceRepository,
            ResourceResourceTypeRepository resourceResourceTypeRepository, ResourceService resourceService,
            CachedUrlUpdaterService cachedUrlUpdaterService, MetadataService metadataService) {
        super(resourceRepository, cachedUrlUpdaterService, metadataService);

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
    public List<ResourceDTO> getAll(
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "", required = false) Optional<String> language,
            @ApiParam(value = "Filter by contentUri") @RequestParam(value = "contentURI", required = false) Optional<URI> contentUri,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "key", required = false) Optional<String> key,
            @ApiParam(value = "Filter by key and value") @RequestParam(value = "value", required = false) Optional<String> value,
            @ApiParam(value = "Filter by visible") @RequestParam(value = "isVisible", required = false) Optional<Boolean> isVisible) {
        MetadataFilters metadataFilters = new MetadataFilters(key, value, isVisible);
        return resourceService.getResources(language, contentUri, metadataFilters);
    }

    @GetMapping("/search")
    @ApiOperation(value = "Search all resources")
    @Transactional(readOnly = true)
    public SearchResultDTO<ResourceDTO> search(
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "", required = false) Optional<String> language,
            @ApiParam(value = "How many results to return per page") @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @ApiParam(value = "Which page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @ApiParam(value = "Query to search names") @RequestParam(value = "query", required = false) Optional<String> query,
            @ApiParam(value = "Ids to fetch for query") @RequestParam(value = "ids", required = false) Optional<List<String>> ids

    ) {
        return resourceService.search(query, ids, language, pageSize, page);
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

    @PostMapping("{id}/clone")
    @ApiOperation(value = "Clones a resource, including resource-types and translations")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> clone(
            @ApiParam(name = "id", value = "Id of resource to clone", example = "urn:resource:1") @PathVariable("id") URI publicId,
            @ApiParam(name = "resource", value = "Object containing contentUri. Other values are ignored.") @RequestBody ResourceCommand command) {
        Resource entity = resourceService.cloneResource(publicId, command.contentUri);
        URI location = URI.create(getLocation() + "/" + entity.getPublicId());
        return ResponseEntity.created(location).build();
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
    public ResourceWithParentsDTO getResourceFull(@PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return resourceService.getResourceWithParentNodesByPublicId(id, language);
    }

    @DeleteMapping("{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        resourceService.delete(id);
    }
}
