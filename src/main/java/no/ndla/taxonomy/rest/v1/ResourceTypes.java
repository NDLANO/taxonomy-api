package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import no.ndla.taxonomy.rest.v1.commands.CreateCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.ndla.taxonomy.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"/v1/resource-types"})
@Transactional
public class ResourceTypes extends CrudController<ResourceType> {

    private ResourceTypeRepository resourceTypeRepository;
    private JdbcTemplate jdbcTemplate;

    private static final String GET_RESOURCE_TYPES_RECURSIVELY_QUERY = getQuery("get_resource_types_recursively");

    public ResourceTypes(ResourceTypeRepository resourceTypeRepository, JdbcTemplate jdbcTemplate) {
        this.resourceTypeRepository = resourceTypeRepository;
        this.jdbcTemplate = jdbcTemplate;
        repository = resourceTypeRepository;
    }

    @GetMapping
    @ApiOperation("Gets a list of all resource types")
    public List<ResourceTypeIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        String sql = GET_RESOURCE_TYPES_RECURSIVELY_QUERY;
        sql = sql.replace("1 = 1", "rt.parent_id is null");
        ResourceTypeQueryExtractor extractor = new ResourceTypeQueryExtractor();
        return jdbcTemplate.query(sql, setQueryParameters(language),
                extractor::extractResourceTypes
        );
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single resource type")
    public ResourceTypeIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        String sql = GET_RESOURCE_TYPES_RECURSIVELY_QUERY;

        sql = sql.replace("1 = 1", "rt.public_id = ?").replace("2 = 2", "t.level = 0");

        ResourceTypeQueryExtractor extractor = new ResourceTypeQueryExtractor();
        return getFirst(jdbcTemplate.query(sql, setQueryParameters(id.toString(), language),
                extractor::extractResourceTypes
        ), "Subject", id);
    }

    @PostMapping
    @ApiOperation(value = "Adds a new resource type")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "resourceType", value = "The new resource type")
            @RequestBody CreateResourceTypeCommand command
    ) throws Exception {
        ResourceType resourceType = new ResourceType();
        if (null != command.parentId) {
            ResourceType parent = resourceTypeRepository.getByPublicId(command.parentId);
            resourceType.setParent(parent);
        }
        return doPost(resourceType, command);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource type. Use to update which resource type is parent. You can also update the id, take care!")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable URI id,
            @ApiParam(name = "resourceType", value = "The updated resource type. Fields not included will be set to null.")
            @RequestBody UpdateResourceTypeCommand
                    command
    ) throws Exception {
        ResourceType resourceType = doPut(id, command);

        ResourceType parent = null;
        if (command.parentId != null) {
            parent = resourceTypeRepository.getByPublicId(command.parentId);
        }
        resourceType.setParent(parent);
        if (command.id != null) {
            resourceType.setPublicId(command.id);
        }
    }


    @GetMapping("/{id}/subtypes")
    @ApiOperation(value = "Gets subtypes of one resource type")
    public List<ResourceTypeIndexDocument> getSubtypes(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
            @ApiParam("If true, sub resource types are fetched recursively")
                    boolean recursive
    ) throws Exception {

        String sql = GET_RESOURCE_TYPES_RECURSIVELY_QUERY;
        List<Object> args = new ArrayList<>();

        sql = sql.replace("1 = 1", "rt.public_id = ?");
        args.add(id.toString());

        if (recursive) {
            sql = sql.replace("2 = 2", "t.level >= 1");
        } else {
            sql = sql.replace("2 = 2", "t.level = 1");
        }

        ResourceTypeQueryExtractor extractor = new ResourceTypeQueryExtractor();
        args.add(language);
        return jdbcTemplate.query(sql, setQueryParameters(args),
                extractor::extractResourceTypes
        );
    }

    @ApiModel("ResourceTypeIndexDocument")
    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:resourcetype:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the resource type", example = "Lecture")
        public String name;

        @JsonProperty
        @ApiModelProperty("Sub resource types")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<ResourceTypeIndexDocument> subtypes = new ArrayList<>();
    }

    public static class CreateResourceTypeCommand extends CreateCommand<ResourceType> {
        @JsonProperty
        @ApiModelProperty(value = "If specified, the new resource type will be a child of the mentioned resource type.")
        public URI parentId;

        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:resourcetype: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:resourcetype:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource type", example = "Lecture")
        public String name;

        @Override
        public URI getId() {
            return id;
        }

        @Override
        public void apply(ResourceType entity) {
            entity.setName(name);
        }
    }

    public static class UpdateResourceTypeCommand extends UpdateCommand<ResourceType> {
        @JsonProperty
        @ApiModelProperty(value = "If specified, this resource type will be a child of the mentioned parent resource type. If left blank, this resource type will become a top level resource type")
        public URI parentId;

        @JsonProperty
        @ApiModelProperty(value = "The name of the resource type", example = "Lecture")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "The id of the resource type", example = "urn:resourcetype:lecture")
        public URI id;

        @Override
        public void apply(ResourceType resourceType) {
            resourceType.setName(name);
        }
    }

    private class ResourceTypeQueryExtractor {

        private List<ResourceTypeIndexDocument> extractResourceTypes(ResultSet resultSet) throws SQLException {
            List<ResourceTypeIndexDocument> result = new ArrayList<>();
            Map<Integer, ResourceTypeIndexDocument> parents = new HashMap<>();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                int parentId = resultSet.getInt("parent_id");

                ResourceTypeIndexDocument resourceType = new ResourceTypeIndexDocument() {{
                    name = resultSet.getString("name");
                    id = getURI(resultSet, "public_id");
                }};
                parents.put(id, resourceType);
                if (parents.containsKey(parentId)) {
                    parents.get(parentId).subtypes.add(resourceType);
                } else {
                    result.add(resourceType);
                }
            }
            return result;
        }
    }
}
