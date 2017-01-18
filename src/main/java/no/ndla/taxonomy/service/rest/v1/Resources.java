package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Resource;
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
@RequestMapping(path = {"resources", "/v1/resources"})
@Transactional
public class Resources {

    private ResourceRepository resourceRepository;

    public Resources(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @GetMapping
    @ApiOperation(value = "Lists all resources",
            notes = "Multiple status values can be provided with comma seperated strings")
    public List<ResourceIndexDocument> index() throws Exception {
        List<ResourceIndexDocument> result = new ArrayList<>();
        resourceRepository.findAll().forEach(record -> result.add(new ResourceIndexDocument(record)));
        return result;
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single resource")
    public ResourceIndexDocument get(@PathVariable("id") URI id) throws Exception {
        Resource resource = resourceRepository.getByPublicId(id);
        ResourceIndexDocument result = new ResourceIndexDocument(resource);
        return result;
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) throws Exception {
        resourceRepository.getByPublicId(id);
        resourceRepository.deleteByPublicId(id);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(@PathVariable("id") URI id, @ApiParam(name = "resource", value = "the updated resource") @RequestBody UpdateResourceCommand command) throws Exception {
        Resource resource = resourceRepository.getByPublicId(id);
        resource.setName(command.name);
    }

    @PostMapping
    @ApiOperation(value = "Adds a new resource")
    public ResponseEntity<Void> post(
            @ApiParam(name = "resource", value = "the new resource") @RequestBody CreateResourceCommand command) throws Exception {
        try {
            Resource resource = new Resource();
            if (null != command.id) resource.setPublicId(command.id);
            resource.name(command.name);
            resourceRepository.save(resource);
            URI location = URI.create("/resources/" + resource.getPublicId());
            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException(command.id.toString());
        }
    }

    public static class CreateResourceCommand {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:resource: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:resource:2")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource", example = "Introduction to integration")
        public String name;
    }

    static class UpdateResourceCommand {
        @JsonProperty
        @ApiModelProperty(value = "The name of the resource", example = "Introduction to integration")
        public String name;
    }

    static class ResourceIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:resource:345")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the resource", example = "Introduction to integration")
        public String name;

        ResourceIndexDocument() {
        }

        ResourceIndexDocument(Resource resource) {
            id = resource.getPublicId();
            name = resource.getName();
        }
    }
}
