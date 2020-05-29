package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.repositories.ResourceFilterRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.rest.v1.commands.CreateResourceCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateResourceCommand;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.MetadataApiService;
import no.ndla.taxonomy.service.ResourceService;
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
import java.util.stream.Collectors;

@RestController
public class Resources extends PathResolvableEntityRestController<Resource> {
    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private final ResourceFilterRepository resourceFilterRepository;
    private final ResourceService resourceService;

    public Resources(ResourceRepository resourceRepository,
                     ResourceResourceTypeRepository resourceResourceTypeRepository,
                     ResourceFilterRepository resourceFilterRepository,
                     ResourceService resourceService, MetadataApiService metadataApiService,
                     CachedUrlUpdaterService cachedUrlUpdaterService) {
        super(resourceRepository, metadataApiService, cachedUrlUpdaterService);

        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
        this.resourceFilterRepository = resourceFilterRepository;
        this.repository = resourceRepository;
        this.resourceService = resourceService;
    }

    @Override
    protected String getLocation() {
        return "/v1/resources";
    }

    @GetMapping("/v1/resources")
    @ApiOperation(value = "Lists all resources")
    @Transactional(readOnly = true)
    public List<ResourceDTO> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,

            @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
            @RequestParam(required = false, defaultValue = "false")
                    boolean includeMetadata
    ) {
        return resourceService.getResources(language, includeMetadata);
    }

    @GetMapping("/v1/resources/{id}")
    @ApiOperation(value = "Gets a single resource")
    public ResourceDTO get(
            @PathVariable("id")
                    URI id,

            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,

            @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
            @RequestParam(required = false, defaultValue = "false")
                    boolean includeMetadata
    ) {

        return resourceService.getResourceByPublicId(id, language, includeMetadata);
    }

    @PutMapping("/v1/resources/{id}")
    @ApiOperation(value = "Updates a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(@PathVariable("id") URI id, @ApiParam(name = "resource", value = "the updated resource. Fields not included will be set to null.")
    @RequestBody UpdateResourceCommand command) {
        doPut(id, command);
    }

    @PostMapping("/v1/resources")
    @ApiOperation(value = "Adds a new resource")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(
            @ApiParam(name = "resource", value = "the new resource") @RequestBody CreateResourceCommand command) {
        return doPost(new Resource(), command);
    }

    @GetMapping("/v1/resources/{id}/resource-types")
    @ApiOperation(value = "Gets all resource types associated with this resource")
    @Transactional(readOnly = true)
    public List<ResourceTypeWithConnectionDTO> getResourceTypes(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {

        return resourceResourceTypeRepository.findAllByResourcePublicIdIncludingResourceAndResourceTypeAndResourceTypeParent(id)
                .stream()
                .map(resourceResourceType -> new ResourceTypeWithConnectionDTO(resourceResourceType, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/v1/resources/{id}/filters")
    @ApiOperation(value = "Gets all filters associated with this resource")
    @Transactional(readOnly = true)
    public List<FilterWithConnectionDTO> getFilters(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return resourceFilterRepository.findAllByResourcePublicIdIncludingResourceAndFilterAndRelevance(id)
                .stream()
                .map(resourceFilter -> new FilterWithConnectionDTO(resourceFilter, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/v1/resources/{id}/full")
    @ApiOperation(value = "Gets all parent topics, all filters and resourceTypes for this resource")
    @Transactional(readOnly = true)
    public ResourceWithParentTopicsDTO getResourceFull(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return resourceService.getResourceWithParentTopicsByPublicId(id, language);
    }


    @DeleteMapping("/v1/resources/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        resourceService.delete(id);
    }

    @GetMapping("/v1/subjects/{subjectId}/resources")
    @ApiOperation(value = "Gets all resources for a subject. Searches recursively in all topics belonging to this subject." +
            "The ordering of resources will be based on the rank of resources relative to the topics they belong to.")
    public List<ResourceWithTopicConnectionDTO> getResourcesForSubject(
            @PathVariable("subjectId") URI subjectId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "type", required = false, defaultValue = "")
            @ApiParam(value = "Filter by resource type id(s). If not specified, resources of all types will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] resourceTypeIds,
            @RequestParam(value = "filter", required = false, defaultValue = "")
            @ApiParam(value = "Select by filter id(s). If not specified, all resources will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false, defaultValue = "")
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance,

            @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
            @RequestParam(required = false, defaultValue = "false")
                    boolean includeMetadata
    ) {
        final Set<URI> filterIdSet = filterIds != null ? Set.of(filterIds) : Set.of();
        final Set<URI> resourceTypeIdSet = resourceTypeIds != null ? Set.of(resourceTypeIds) : Set.of();

        // If null is sent to query it will be ignored, otherwise it will filter by relevance
        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        return resourceService.getResourcesBySubjectId(subjectId, filterIdSet, resourceTypeIdSet, relevanceArgument, language, includeMetadata);
    }

    @GetMapping("/v1/topics/{id}/resources")
    @ApiOperation(value = "Gets all resources for the given topic")
    public List<ResourceWithTopicConnectionDTO> getResources(
            @ApiParam(value = "id", required = true)
            @PathVariable("id") URI topicId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false)
                    String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
            @ApiParam("If true, resources from subtopics are fetched recursively")
                    boolean recursive,
            @RequestParam(value = "type", required = false)
            @ApiParam(value = "Select by resource type id(s). If not specified, resources of all types will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] resourceTypeIds,
            @RequestParam(value = "subject", required = false)
            @ApiParam(value = "Select filters by subject id if filter list is empty. Used as alternative to specify filters.")
                    URI subjectId,
            @RequestParam(value = "filter", required = false)
            @ApiParam(value = "Select by filter id(s). If not specified, all resources will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false)
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance,

            @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
            @RequestParam(required = false)
                    boolean includeMetadata
    ) {
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

        return resourceService.getResourcesByTopicId(topicId, filterIdSet, subjectId, resourceTypeIdSet,
                relevance, language, recursive, includeMetadata);
    }

}
