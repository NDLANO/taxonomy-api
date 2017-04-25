package no.ndla.taxonomy.service.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.*;
import no.ndla.taxonomy.service.repositories.FilterRepository;
import no.ndla.taxonomy.service.repositories.RelevanceRepository;
import no.ndla.taxonomy.service.repositories.ResourceFilterRepository;
import no.ndla.taxonomy.service.repositories.ResourceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.net.URI;

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

    public static class AddFilterToResourceCommand {
        public URI resourceId;
        public URI filterId;
        public URI relevanceId;
    }
}
