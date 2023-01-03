/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.rest.v1.commands.ResourceCommand;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.MetadataFilters;
import no.ndla.taxonomy.service.MetadataService;
import no.ndla.taxonomy.service.ResourceService;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import no.ndla.taxonomy.service.dtos.ResourceTypeWithConnectionDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithParentsDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.data.domain.PageRequest;
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

    private final ResourceRepository resourceRepository;

    public Resources(ResourceRepository resourceRepository,
            ResourceResourceTypeRepository resourceResourceTypeRepository, ResourceService resourceService,
            CachedUrlUpdaterService cachedUrlUpdaterService, MetadataService metadataService) {
        super(resourceRepository, cachedUrlUpdaterService, metadataService);

        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
        this.repository = resourceRepository;
        this.resourceRepository = resourceRepository;
        this.resourceService = resourceService;
    }

    @Override
    protected String getLocation() {
        return "/v1/resources";
    }

    @GetMapping
    @Operation(summary = "Lists all resources")
    @Transactional(readOnly = true)
    public List<ResourceDTO> getAll(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "", required = false) Optional<String> language,
            @Parameter(description = "Filter by contentUri") @RequestParam(value = "contentURI", required = false) Optional<URI> contentUri,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "key", required = false) Optional<String> key,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "value", required = false) Optional<String> value,
            @Parameter(description = "Filter by visible") @RequestParam(value = "isVisible", required = false) Optional<Boolean> isVisible) {
        MetadataFilters metadataFilters = new MetadataFilters(key, value, isVisible);
        return resourceService.getResources(language, contentUri, metadataFilters);
    }

    @GetMapping("/search")
    @Operation(summary = "Search all resources")
    @Transactional(readOnly = true)
    public SearchResultDTO<ResourceDTO> search(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "", required = false) Optional<String> language,
            @Parameter(description = "How many results to return per page") @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @Parameter(description = "Which page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Query to search names") @RequestParam(value = "query", required = false) Optional<String> query,
            @Parameter(description = "Ids to fetch for query") @RequestParam(value = "ids", required = false) Optional<List<String>> ids

    ) {
        return resourceService.search(query, ids, language, pageSize, page);
    }

    @GetMapping("/page")
    @Operation(summary = "Gets all connections between node and children paginated")
    public SearchResultDTO<ResourceDTO> allPaginated(
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", defaultValue = "", required = false) Optional<String> language,
            @Parameter(name = "page", description = "The page to fetch") Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch") Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1)
            throw new IllegalArgumentException("page parameter must be bigger than 0");

        var ids = resourceRepository.findIdsPaginated(PageRequest.of(page.get() - 1, pageSize.get()));
        var results = resourceRepository.findByIds(ids.getContent());
        var contents = results.stream().map(node -> new ResourceDTO(node, language.orElse("nb")))
                .collect(Collectors.toList());
        return new SearchResultDTO<>(ids.getTotalElements(), page.get(), pageSize.get(), contents);
    }

    @GetMapping("{id}")
    @Operation(summary = "Gets a single resource")
    public ResourceDTO get(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {

        return resourceService.getResourceByPublicId(id, language);
    }

    @PutMapping("{id}")
    @Operation(summary = "Updates a resource", security = { @SecurityRequirement(name = "oauth") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(@PathVariable("id") URI id,
            @Parameter(name = "resource", description = "the updated resource. Fields not included will be set to null.") @RequestBody ResourceCommand command) {
        doPut(id, command);
    }

    @PostMapping
    @Operation(summary = "Adds a new resource", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(
            @Parameter(name = "resource", description = "the new resource") @RequestBody ResourceCommand command) {
        return doPost(new Resource(), command);
    }

    @PostMapping("{id}/clone")
    @Operation(summary = "Clones a resource, including resource-types and translations", security = {
            @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> clone(
            @Parameter(name = "id", description = "Id of resource to clone", example = "urn:resource:1") @PathVariable("id") URI publicId,
            @Parameter(name = "resource", description = "Object containing contentUri. Other values are ignored.") @RequestBody ResourceCommand command) {
        Resource entity = resourceService.cloneResource(publicId, command.contentUri);
        URI location = URI.create(getLocation() + "/" + entity.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @GetMapping("{id}/resource-types")
    @Operation(summary = "Gets all resource types associated with this resource")
    @Transactional(readOnly = true)
    public List<ResourceTypeWithConnectionDTO> getResourceTypes(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {

        return resourceResourceTypeRepository
                .findAllByResourcePublicIdIncludingResourceAndResourceTypeAndResourceTypeParent(id).stream()
                .map(resourceResourceType -> new ResourceTypeWithConnectionDTO(resourceResourceType, language))
                .collect(Collectors.toList());
    }

    @GetMapping("{id}/full")
    @Operation(summary = "Gets all parent topics, all filters and resourceTypes for this resource")
    @Transactional(readOnly = true)
    public ResourceWithParentsDTO getResourceFull(@PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb") @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        return resourceService.getResourceWithParentNodesByPublicId(id, language);
    }

    @DeleteMapping("{id}")
    @Operation(summary = "Deletes a single entity by id", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        resourceService.delete(id);
    }
}
