package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.ResourceType;
import no.ndla.taxonomy.service.repositories.ResourceTypeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"resource-types", "/v1/resource-types"})
@Transactional
public class ResourceTypes {

    private ResourceTypeRepository resourceTypeRepository;

    public ResourceTypes(ResourceTypeRepository resourceTypeRepository) {
        this.resourceTypeRepository = resourceTypeRepository;
    }

    @GetMapping
    @ApiOperation("Gets a list of all resource types")
    public List<ResourceTypeIndexDocument> index() throws Exception {
        List<ResourceTypeIndexDocument> result = new ArrayList<>();
        resourceTypeRepository.findAll().forEach(record -> result.add(new ResourceTypeIndexDocument(record)));
        return result;
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single resource type")
    public ResourceTypeIndexDocument get(@PathVariable("id") URI id) throws Exception {
        return new ResourceTypeIndexDocument(resourceTypeRepository.getByPublicId(id));
    }

    @PostMapping
    @ApiOperation(value = "Adds a new resource type")
    public ResponseEntity<Void> post(
            @ApiParam(name = "resourceType", value = "The new resource type")
            @RequestBody
                    CreateResourceTypeCommand command
    ) throws Exception {
        try {
            ResourceType resourceType = new ResourceType();
            if (null != command.id) resourceType.setPublicId(command.id);

            if (null != command.parentId) {
                ResourceType parent = resourceTypeRepository.getByPublicId(command.parentId);
                resourceType.setParent(parent);
            }
            resourceType.name(command.name);
            resourceTypeRepository.save(resourceType);
            URI location = URI.create("/resource-types/" + resourceType.getPublicId());
            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException(command.id.toString());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Deletes a single resource type")
    public void delete(@PathVariable("id") URI id) throws Exception {
        resourceTypeRepository.getByPublicId(id);
        resourceTypeRepository.deleteByPublicId(id);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource type. Use to update which resource type is parent.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable URI id,
            @ApiParam(name = "resourceType", value = "The updated resource type") @RequestBody UpdateResourceTypeCommand command
    ) throws Exception {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        resourceType.name(command.name);
        ResourceType parent = null;
        if (command.parentId != null) {
            parent = resourceTypeRepository.getByPublicId(command.parentId);
        }
        resourceType.setParent(parent);
    }

    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:resource-type:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource type", example = "Lecture")
        public String name;

        ResourceTypeIndexDocument() {
        }

        ResourceTypeIndexDocument(ResourceType resourceType) {
            id = resourceType.getPublicId();
            name = resourceType.getName();
        }
    }

    public static class CreateResourceTypeCommand {
        @JsonProperty
        @ApiModelProperty(value = "If specified, the new resource type will be a child of the mentioned resource type.")
        public URI parentId;

        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:resource-type: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:resource-type:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource type", example = "Lecture")
        public String name;
    }

    public static class UpdateResourceTypeCommand {
        @JsonProperty
        @ApiModelProperty(value = "If specified, this resource type will be a child of the mentioned parent resource type. If left blank, this resource type will become a top level resource type")
        public URI parentId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource type", example = "Lecture")
        public String name;
    }
}
