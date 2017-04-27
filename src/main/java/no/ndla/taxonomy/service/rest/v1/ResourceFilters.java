package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.*;
import no.ndla.taxonomy.service.repositories.FilterRepository;
import no.ndla.taxonomy.service.repositories.RelevanceRepository;
import no.ndla.taxonomy.service.repositories.ResourceFilterRepository;
import no.ndla.taxonomy.service.repositories.ResourceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/resource-filters"})
@Transactional
public class ResourceFilters {
    private FilterRepository filterRepository;
    private ResourceFilterRepository resourceFilterRepository;
    private ResourceRepository resourceRepository;
    private RelevanceRepository relevanceRepository;

    public ResourceFilters(FilterRepository filterRepository, ResourceRepository resourceRepository, ResourceFilterRepository resourceFilterRepository, RelevanceRepository relevanceRepository) {
        this.filterRepository = filterRepository;
        this.resourceRepository = resourceRepository;
        this.resourceFilterRepository = resourceFilterRepository;
        this.relevanceRepository = relevanceRepository;
    }

    @PostMapping
    @ApiOperation(value = "Adds a filter to a resource")
    public ResponseEntity<Void> post(@ApiParam(name = "topic", value = "The new connection") @RequestBody AddFilterToResourceCommand command) throws Exception {
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
    public void put(@PathVariable("id") URI id, @ApiParam(name = "resource filter", value = "the updated resource filter") @RequestBody UpdateResourceFilterCommand command) throws Exception {
        ResourceFilter resourceFilter = resourceFilterRepository.getByPublicId(id);
        Relevance relevance = relevanceRepository.getByPublicId(command.relevanceId);
        resourceFilter.setRelevance(relevance);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@ApiParam(name = "id", value = "The id of the connection to delete") @PathVariable String id) {
        ResourceFilter resourceFilter = resourceFilterRepository.getByPublicId(URI.create(id));
        resourceFilter.getResource().removeFilter(resourceFilter.getFilter());
        resourceFilterRepository.deleteByPublicId(URI.create(id));
    }

    @GetMapping
    @ApiOperation("Gets all connections between resources and filters")
    public List<ResourceFilterIndexDocument> index() throws Exception {
        List<ResourceFilterIndexDocument> result = new ArrayList<>();
        resourceFilterRepository.findAll().forEach(record -> result.add(new ResourceFilterIndexDocument(record)));
        return result;
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
            relevanceId = resourceFilter.getRelevance().getPublicId();
        }
    }
}
