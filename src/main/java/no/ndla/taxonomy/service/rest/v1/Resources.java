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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"resources", "/v1/resources"})
@Transactional
public class Resources {

    private static final String GET_RESOURCES_QUERY = getQuery("get_resources");
    private static final String GET_RESOURCE_RESOURCE_TYPES_QUERY = getQuery("get_resource_resource_types");
    private static final String GET_FILTERS_BY_RESOURCE_ID_QUERY = getQuery("get_filters_by_resource_public_id");

    private ResourceRepository resourceRepository;
    private JdbcTemplate jdbcTemplate;

    public Resources(ResourceRepository resourceRepository, JdbcTemplate jdbcTemplate) {
        this.resourceRepository = resourceRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @ApiOperation(value = "Lists all resources")
    public List<ResourceIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        return getResourceIndexDocuments(GET_RESOURCES_QUERY, singletonList(language));
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single resource")
    public ResourceIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        String sql = GET_RESOURCES_QUERY.replace("1 = 1", "r.public_id = ?");
        List<Object> args = asList(language, id.toString());

        return getFirst(getResourceIndexDocuments(sql, args), "Resource", id);
    }

    private List<ResourceIndexDocument> getResourceIndexDocuments(String sql, List<Object> args) {
        return jdbcTemplate.query(sql, setQueryParameters(args),
                resultSet -> {
                    List<ResourceIndexDocument> result = new ArrayList<>();
                    while (resultSet.next()) {
                        result.add(new ResourceIndexDocument() {{
                            name = resultSet.getString("resource_name");
                            id = getURI(resultSet, "resource_public_id");
                            contentUri = getURI(resultSet, "resource_content_uri");
                            path = resultSet.getString("resource_path");
                        }});
                    }
                    return result;
                }
        );
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) throws Exception {
        Resource resource = resourceRepository.getByPublicId(id);
        resourceRepository.delete(resource);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(@PathVariable("id") URI id, @ApiParam(name = "resource", value = "the updated resource") @RequestBody UpdateResourceCommand command) throws Exception {
        Resource resource = resourceRepository.getByPublicId(id);
        resource.setName(command.name);
        resource.setContentUri(command.contentUri);
    }

    @PostMapping
    @ApiOperation(value = "Adds a new resource")
    public ResponseEntity<Void> post(
            @ApiParam(name = "resource", value = "the new resource") @RequestBody CreateResourceCommand command) throws Exception {
        try {
            Resource resource = new Resource();
            if (null != command.id) resource.setPublicId(command.id);
            resource.name(command.name);
            resource.setContentUri(command.contentUri);
            resourceRepository.save(resource);
            URI location = URI.create("/resources/" + resource.getPublicId());
            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException("" + command.id);
        }
    }

    @GetMapping("/{id}/resource-types")
    @ApiOperation(value = "Gets all resource types associated with this resource")
    public List<ResourceTypeIndexDocument> getResourceTypes(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        return jdbcTemplate.query(GET_RESOURCE_RESOURCE_TYPES_QUERY, setQueryParameters(asList(language, id.toString())),
                resultSet -> {
                    List<ResourceTypeIndexDocument> result = new ArrayList<>();
                    while (resultSet.next()) {
                        result.add(new ResourceTypeIndexDocument() {{
                            name = resultSet.getString("resource_type_name");
                            id = getURI(resultSet, "resource_type_public_id");
                            parentId = getURI(resultSet, "resource_type_parent_public_id");
                            connectionId = getURI(resultSet, "resource_resource_type_public_id");
                        }});
                    }
                    return result;
                }
        );
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters associated with this resource")
    public List<FilterIndexDocument> getFilters(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        return jdbcTemplate.query(GET_FILTERS_BY_RESOURCE_ID_QUERY, setQueryParameters(singletonList(id.toString())),
                resultSet -> {
                    List<FilterIndexDocument> result = new ArrayList<>();
                    while (resultSet.next()) {
                        result.add(new FilterIndexDocument() {{
                            name = resultSet.getString("filter_name");
                            id = getURI(resultSet, "filter_public_id");
                            connectionId = getURI(resultSet, "filter_resource_public_id");
                            relevanceId = getURI(resultSet, "relevance_id");
                        }});
                    }
                    return result;
                }
        );
    }

    public static class CreateResourceCommand {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:resource: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:resource:2")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The ID of this resource in the system where the content is stored.",
                notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                        "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource", example = "Introduction to integration")
        public String name;
    }

    static class UpdateResourceCommand {
        @JsonProperty
        @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
                notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                        "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
        public URI contentUri;

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

        @JsonProperty
        @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
                notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                        "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "The path part of the url to this resource", example = "/subject:1/topic:1/resource:1")
        public String path;
    }

    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:resource-type:2")
        public URI id;

        @JsonProperty
        @ApiModelProperty(example = "urn:resource-type:1")
        public URI parentId;

        @JsonProperty
        @ApiModelProperty(value = "The name of the resource type", example = "Lecture")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "The id of the resource resource type connection", example = "urn:resource-resourcetype:1")
        public URI connectionId;
    }

    public static class FilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:filter:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the filter", example = "1T-YF")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "The id of the filter resource connection", example = "urn:filter-resource:1")
        public URI connectionId;

        @JsonProperty
        @ApiModelProperty(value = "The relevance of this resource according to the filter", example = "urn:relevance:1")
        public URI relevanceId;
    }
}
