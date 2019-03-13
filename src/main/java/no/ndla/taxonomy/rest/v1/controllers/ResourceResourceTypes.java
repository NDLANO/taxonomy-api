package no.ndla.taxonomy.rest.v1.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceResourceType;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/resource-resourcetypes"})
@Transactional
public class ResourceResourceTypes {

    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private ResourceTypeRepository resourceTypeRepository;
    private final ResourceRepository resourceRepository;

    public ResourceResourceTypes(ResourceResourceTypeRepository resourceResourceTypeRepository, ResourceTypeRepository resourceTypeRepository, ResourceRepository resourceRepository) {
        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.resourceRepository = resourceRepository;
    }

    @PostMapping
    @ApiOperation(value = "Adds a resource type to a resource")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new resource/resource type connection") @RequestBody CreateResourceResourceTypeCommand command) throws Exception {

        Resource resource = resourceRepository.getByPublicId(command.resourceId);
        ResourceType resourceType = resourceTypeRepository.getByPublicId(command.resourceTypeId);

        ResourceResourceType resourceResourceType = resource.addResourceType(resourceType);
        resourceResourceTypeRepository.save(resourceResourceType);

        URI location = URI.create("/resource-resourcetypes/" + resourceResourceType.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping({"/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Removes a resource type from a resource")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) throws Exception {
        ResourceResourceType resourceResourceType = resourceResourceTypeRepository.getByPublicId(id);
        resourceResourceType.getResource().removeResourceType(resourceResourceType.getResourceType());
        resourceResourceTypeRepository.delete(resourceResourceType);
    }

    @GetMapping
    @ApiOperation("Gets all connections between resources and resource types")
    public List<ResourceResourceTypeIndexDocument> index() throws Exception {
        List<ResourceResourceTypeIndexDocument> result = new ArrayList<>();
        resourceResourceTypeRepository.findAll().forEach(record -> result.add(new ResourceResourceTypeIndexDocument(record)));
        return result;
    }

    @GetMapping({"/{id}"})
    @ApiOperation("Gets a single connection between resource and resource type")
    public ResourceResourceTypeIndexDocument get(@PathVariable("id") URI id) throws Exception {
        ResourceResourceType result = resourceResourceTypeRepository.getByPublicId(id);
        return new ResourceResourceTypeIndexDocument(result);
    }

    public static class CreateResourceResourceTypeCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:123")
        URI resourceId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;
    }

    @ApiModel("ResourceTypeIndexDocument")
    public static class ResourceResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resource:123")
        URI resourceId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource to resource type connection id", example = "urn:resource-has-resourcetypes:12")
        URI id;

        public ResourceResourceTypeIndexDocument() {
        }

        public ResourceResourceTypeIndexDocument(ResourceResourceType resourceResourceType) {
            id = resourceResourceType.getPublicId();
            resourceId = resourceResourceType.getResource().getPublicId();
            resourceTypeId = resourceResourceType.getResourceType().getPublicId();
        }
    }
}
