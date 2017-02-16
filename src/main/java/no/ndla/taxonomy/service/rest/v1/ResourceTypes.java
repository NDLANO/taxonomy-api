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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"resource-types", "/v1/resource-types"})
@Transactional
public class ResourceTypes {

    private ResourceTypeRepository resourceTypeRepository;
    private JdbcTemplate jdbcTemplate;

    private static final String GET_RESOURCE_TYPES_QUERY = getQuery("get_resource_types");

    public ResourceTypes(ResourceTypeRepository resourceTypeRepository, JdbcTemplate jdbcTemplate) {
        this.resourceTypeRepository = resourceTypeRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @ApiOperation("Gets a list of all resource types")
    public List<ResourceTypeIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        return getResourceTypeIndexDocuments(GET_RESOURCE_TYPES_QUERY, singletonList(language));
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single resource type")
    public ResourceTypeIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        String sql = GET_RESOURCE_TYPES_QUERY.replace("1 = 1", "rt.public_id = ?");
        List<Object> args = asList(language, id.toString());

        return getFirst(getResourceTypeIndexDocuments(sql, args), "Subject", id);
    }

    private List<ResourceTypeIndexDocument> getResourceTypeIndexDocuments(String sql, List<Object> args) {
        return jdbcTemplate.query(sql, setQueryParameters(args),
                resultSet -> {
                    List<ResourceTypeIndexDocument> result = new ArrayList<>();
                    while (resultSet.next()) {
                        result.add(new ResourceTypeIndexDocument() {{
                            name = resultSet.getString("resource_type_name");
                            id = getURI(resultSet, "resource_type_public_id");
                        }});
                    }
                    return result;
                }
        );
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
            @ApiParam(name = "resourceType", value = "The updated resource type")
            @RequestBody UpdateResourceTypeCommand
                    command
    ) throws Exception {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        resourceType.name(command.name);
        ResourceType parent = null;
        if (command.parentId != null) {
            parent = resourceTypeRepository.getByPublicId(command.parentId);
        }
        resourceType.setParent(parent);
    }


    @GetMapping("/{id}/subresourcetypes")
    public List<ResourceTypeIndexDocument> getSubResourceTypes(
            @PathVariable("id") URI id,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
            @ApiParam("If true, sub resource types are fetched recursively")
                    boolean recursive
    ) throws Exception {

        // TODO: Language

        List<ResourceTypeIndexDocument> results = new ArrayList<>();
        final Iterator<ResourceType> subtypes = resourceTypeRepository.getByPublicId(id).getSubtypes();
        subtypes.forEachRemaining(rt -> results.add(new ResourceTypeIndexDocument(rt, recursive)));
        return results;
    }

    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:resource-type:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the resource type", example = "Lecture")
        public String name;

        @JsonProperty
        @ApiModelProperty("Sub resource types")
        public ResourceTypeIndexDocument[] subResourceTypes;

        ResourceTypeIndexDocument() {
        }

        ResourceTypeIndexDocument(ResourceType resourceType) {
            id = resourceType.getPublicId();
            name = resourceType.getName();
        }

        ResourceTypeIndexDocument(ResourceType resourceType, boolean recursive) {
            id = resourceType.getPublicId();
            name = resourceType.getName();
            if (recursive) {
                addSubResourceType(resourceType);
            }
        }

        private void addSubResourceType(ResourceType resourceType) {
            ArrayList<ResourceTypeIndexDocument> result = new ArrayList<>();

            final Iterator<ResourceType> subtypes = resourceType.getSubtypes();
            while (subtypes.hasNext()) {
                result.add(new ResourceTypeIndexDocument(subtypes.next()));
            }
            this.subResourceTypes = result.toArray(new ResourceTypeIndexDocument[result.size()]);
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
        @ApiModelProperty(value = "The name of the resource type", example = "Lecture")
        public String name;
    }
}
