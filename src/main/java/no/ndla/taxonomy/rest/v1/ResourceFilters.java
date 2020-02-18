package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceFilter;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.repositories.ResourceFilterRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/resource-filters"})
@Transactional
public class ResourceFilters {
    private final FilterRepository filterRepository;
    private final ResourceFilterRepository resourceFilterRepository;
    private final ResourceRepository resourceRepository;
    private final RelevanceRepository relevanceRepository;

    public ResourceFilters(FilterRepository filterRepository, ResourceRepository resourceRepository, ResourceFilterRepository resourceFilterRepository, RelevanceRepository relevanceRepository) {
        this.filterRepository = filterRepository;
        this.resourceRepository = resourceRepository;
        this.resourceFilterRepository = resourceFilterRepository;
        this.relevanceRepository = relevanceRepository;
    }

    @PostMapping
    @ApiOperation(value = "Adds a filter to a resource")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "resource filter", value = "The new resource filter") @RequestBody AddFilterToResourceCommand command) {
        try {
            Filter filter = filterRepository.getByPublicId(command.filterId);
            Resource resource = resourceRepository.getByPublicId(command.resourceId);
            Relevance relevance = relevanceRepository.getByPublicId(command.relevanceId);

            ResourceFilter resourceFilter = resource.addFilter(filter, relevance);
            resourceFilterRepository.save(resourceFilter);

            URI location = URI.create("/v1/resource-filters/" + resourceFilter.getPublicId());
            return ResponseEntity.created(location).build();

        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException("Resource " + command.resourceId + " is already associated with filter " + command.filterId);
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource filter connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id, @ApiParam(name = "resource filter", value = "The updated resource filter", required = true) @RequestBody UpdateResourceFilterCommand command) {
        ResourceFilter resourceFilter = resourceFilterRepository.getByPublicId(id);
        Relevance relevance = relevanceRepository.getByPublicId(command.relevanceId);

        final var resource = resourceFilter.getResource();
        final var filter = resourceFilter.getFilter();

        final var connectionId = resourceFilter.getPublicId();

        // Delete old object and create new as it is not possible to change the old connection object
        resourceFilterRepository.delete(resourceFilter);
        resourceFilterRepository.flush();

        final var newResourceFilter = ResourceFilter.create(resource, filter, relevance);
        newResourceFilter.setPublicId(connectionId);

        resourceFilterRepository.saveAndFlush(newResourceFilter);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@ApiParam(name = "id", value = "The id of the connection to delete", required = true) @PathVariable URI id) {
        ResourceFilter resourceFilter = resourceFilterRepository.getByPublicId(id);
        resourceFilter.getResource().removeFilter(resourceFilter.getFilter());
        resourceFilterRepository.deleteByPublicId(id);
    }

    @GetMapping("/{id}")
    public ResourceFilterIndexDocument get(@ApiParam(name = "id", value = "The id of the connection to get", required = true) @PathVariable URI id) {
        ResourceFilter resourceFilter = resourceFilterRepository.getByPublicId(id);
        return new ResourceFilterIndexDocument(resourceFilter);
    }

    @GetMapping
    @ApiOperation("Gets all connections between resources and filters")
    public List<ResourceFilterIndexDocument> index() {
        return resourceFilterRepository
                .findAllIncludingResourceAndFilterAndRelevance()
                .stream()
                .map(ResourceFilterIndexDocument::new)
                .collect(Collectors.toList());
    }


    public static class AddFilterToResourceCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:123")
        public URI resourceId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;
    }

    public static class UpdateResourceFilterCommand {
        public URI relevanceId;
    }

    @ApiModel("ResourceFilterIndexDocument")
    public static class ResourceFilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:123")
        public URI resourceId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Filter id", example = "urn:filter:234")
        public URI filterId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource to filter connection id", example = "urn:resource-filter:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Relevance id", example = "urn:relevance:core")
        public URI relevanceId;

        public ResourceFilterIndexDocument() {
        }

        public ResourceFilterIndexDocument(ResourceFilter resourceFilter) {
            id = resourceFilter.getPublicId();
            resourceId = resourceFilter.getResource().getPublicId();
            filterId = resourceFilter.getFilter().getPublicId();
            relevanceId = resourceFilter.getRelevance().map(Relevance::getPublicId).orElse(null);
        }
    }
}
