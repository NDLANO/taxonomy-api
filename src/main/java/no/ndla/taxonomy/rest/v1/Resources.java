package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.ResourceFilterRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.rest.v1.commands.CreateResourceCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateResourceCommand;
import no.ndla.taxonomy.rest.v1.dtos.resources.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/resources"})
@Transactional
public class Resources extends CrudController<Resource> {
    private final ResourceRepository resourceRepository;
    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private final ResourceFilterRepository resourceFilterRepository;

    public Resources(ResourceRepository resourceRepository,
                     ResourceResourceTypeRepository resourceResourceTypeRepository,
                     ResourceFilterRepository resourceFilterRepository) {
        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
        this.resourceFilterRepository = resourceFilterRepository;
        this.resourceRepository = resourceRepository;
        this.repository = resourceRepository;
    }

    @GetMapping
    @ApiOperation(value = "Lists all resources")
    public List<ResourceIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {

        return resourceRepository.findAllIncludingCachedUrlsAndTranslations()
                .stream()
                .flatMap(resource -> {
                    // Return single object with null path if there is no primary paths
                    // If primary paths exists, return one object for each primary path found

                    if (resource.getPrimaryPath().isEmpty()) {
                        return Set.of(new ResourceIndexDocument(resource, language)).stream();
                    }

                    return resource.getCachedUrls()
                            .stream()
                            .filter(CachedUrl::isPrimary)
                            .map(cachedUrl -> {
                                final var resourceIndexDocument = new ResourceIndexDocument(resource, language);
                                resource.getPrimaryPath().ifPresent(resourceIndexDocument::setPath);

                                return resourceIndexDocument;
                            });
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single resource")
    public ResourceIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language) {

        final var resource = resourceRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id)
                .orElseThrow(() -> new NotFoundHttpRequestException("No such resource found"));

        return new ResourceWithPathsIndexDocument(resource, language);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id, @ApiParam(name = "resource", value = "the updated resource. Fields not included will be set to null.")
    @RequestBody UpdateResourceCommand command) {
        doPut(id, command);
    }

    @PostMapping
    @ApiOperation(value = "Adds a new resource")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "resource", value = "the new resource") @RequestBody CreateResourceCommand command) {
        return doPost(new Resource(), command);
    }

    @GetMapping("/{id}/resource-types")
    @ApiOperation(value = "Gets all resource types associated with this resource")
    public List<ResourceTypeIndexDocument> getResourceTypes(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {

        return resourceResourceTypeRepository.findAllByResourcePublicIdIncludingResourceAndResourceTypeAndResourceTypeParent(id)
                .stream()
                .map(resourceResourceType -> new ResourceTypeIndexDocument(resourceResourceType, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters associated with this resource")
    public List<FilterIndexDocument> getFilters(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return resourceFilterRepository.findAllByResourcePublicIdIncludingResourceAndFilterAndRelevance(id)
                .stream()
                .map(resourceFilter -> new FilterIndexDocument(resourceFilter, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/full")
    @ApiOperation(value = "Gets all parent topics, all filters and resourceTypes for this resource")
    public ResourceFullIndexDocument getResourceFull(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        final var resource = resourceRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id).orElseThrow(() -> new NotFoundHttpRequestException("Resource not found"));

        final ResourceIndexDocument resourceIndexDocument;

        if (language == null || language.equals("")) {
            resourceIndexDocument = new ResourceIndexDocument(resource);
        } else {
            resourceIndexDocument = new ResourceIndexDocument(resource, language);
        }

        List<ResourceTypeIndexDocument> resourceTypes = new ArrayList<>();
        List<FilterIndexDocument> filters = new ArrayList<>();
        List<ParentTopicIndexDocument> topics = new ArrayList<>();

        resource.getResourceResourceTypes().stream()
                .map(resourceResourceType -> this.createResourceTypeDocument(resourceResourceType, language))
                .forEach(resourceTypes::add);

        resource.getResourceFilters().stream()
                .map(resourceFilter -> this.createFilterIndexDocument(resourceFilter, language))
                .forEach(filters::add);

        resource.getTopicResources().stream()
                .map(topicResource -> this.createParentTopicIndexDocument(topicResource, language))
                .forEach(topics::add);

        ResourceFullIndexDocument r = ResourceFullIndexDocument.from(resourceIndexDocument);
        r.resourceTypes.addAll(resourceTypes);
        r.filters.addAll(filters);
        r.parentTopics.addAll(topics);
        r.paths.addAll(resource.getAllPaths());
        return r;
    }


    private ResourceTypeIndexDocument createResourceTypeDocument(ResourceResourceType resourceResourceType, String languageCode) {
        final var resourceType = resourceResourceType.getResourceType();

        return new ResourceTypeIndexDocument() {{
            name = resourceType.getTranslation(languageCode).map(ResourceTypeTranslation::getName).orElse(resourceType.getName());
            id = resourceType.getPublicId();
            parentId = resourceType.getParent().map(DomainEntity::getPublicId).orElse(null);
            connectionId = resourceResourceType.getPublicId();
        }};
    }

    private FilterIndexDocument createFilterIndexDocument(ResourceFilter resourceFilter, String languageCode) {
        final var filter = resourceFilter.getFilter();
        return new FilterIndexDocument() {{
            name = filter.getTranslation(languageCode).map(FilterTranslation::getName).orElse(filter.getName());
            id = filter.getPublicId();
            connectionId = resourceFilter.getPublicId();
            relevanceId = resourceFilter.getRelevance().map(Relevance::getPublicId).orElse(null);
        }};
    }

    private ParentTopicIndexDocument createParentTopicIndexDocument(TopicResource topicResource, String languageCode) {
        final var topic = topicResource.getTopic().orElseThrow(() -> new IllegalArgumentException("Topic is not set"));

        return new ParentTopicIndexDocument() {{
            name = topic.getTranslation(languageCode).map(TopicTranslation::getName).orElse(topic.getName());
            id = topic.getPublicId();
            isPrimary = topicResource.isPrimary();
            try {
                contentUri = topic.getContentUri() != null ? topic.getContentUri() : new URI("");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            connectionId = topicResource.getPublicId();
        }};
    }
}
