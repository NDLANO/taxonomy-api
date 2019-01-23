package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.rest.v1.command.CreateResourceCommand;
import no.ndla.taxonomy.rest.v1.command.UpdateResourceCommand;
import no.ndla.taxonomy.rest.v1.dto.resources.*;
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
import java.util.Collections;
import java.util.List;

import static no.ndla.taxonomy.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"/v1/resources"})
@Transactional
public class Resources extends CrudController<Resource> {

    private static final String GET_RESOURCES_QUERY = getQuery("get_resources");
    private static final String GET_RESOURCE_WITH_PATHS_QUERY = getQuery("get_resource_with_all_paths");
    private static final String GET_RESOURCE_RESOURCE_TYPES_QUERY = getQuery("get_resource_resource_types");
    private static final String GET_FILTERS_BY_RESOURCE_ID_QUERY = getQuery("get_filters_by_resource_public_id");
    private static final String GET_TOPICS_FOR_RESOURCE = getQuery("get_topics_for_resource");

    private JdbcTemplate jdbcTemplate;

    public Resources(ResourceRepository resourceRepository, JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        repository = resourceRepository;
    }

    @GetMapping
    @ApiOperation(value = "Lists all resources")
    public List<ResourceIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        return getResourceIndexDocuments(GET_RESOURCES_QUERY, language);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single resource")
    public ResourceIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language) {
        ResourceWithPathsIndexDocument result = jdbcTemplate.query(GET_RESOURCE_WITH_PATHS_QUERY, setQueryParameters(language, id.toString()),
                this::extractResourceWithPaths);
        if (result == null) {
            throw new NotFoundHttpRequestException("No such resource found");
        }
        return result;

    }

    private List<ResourceIndexDocument> getResourceIndexDocuments(String sql, Object... args) {
        return jdbcTemplate.query(sql, setQueryParameters(args),
                (resultSet, rowNum) -> new ResourceIndexDocument() {{
                    name = resultSet.getString("resource_name");
                    id = getURI(resultSet, "resource_public_id");
                    contentUri = getURI(resultSet, "resource_content_uri");
                    path = resultSet.getString("resource_path");
                }}
        );
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
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return jdbcTemplate.query(GET_RESOURCE_RESOURCE_TYPES_QUERY, setQueryParameters(language, id.toString()),
                this::extractResourceTypes
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
    ) {
        return jdbcTemplate.query(GET_FILTERS_BY_RESOURCE_ID_QUERY, setQueryParameters(id.toString()),
                this::extractFilters
        );
    }

    @GetMapping("/{id}/full")
    @ApiOperation(value = "Gets all parent topics, all filters and resourceTypes for this resource")
    public ResourceFullIndexDocument getResourceFull(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        String sql = GET_RESOURCES_QUERY.replace("1 = 1", "r.public_id = ?");

        ResourceIndexDocument resource = getFirst(getResourceIndexDocuments(sql, language, id.toString()), "Resource", id);

        List<ResourceTypeIndexDocument> resourceTypes = jdbcTemplate.query(GET_RESOURCE_RESOURCE_TYPES_QUERY, setQueryParameters(language, id.toString()),
                this::extractResourceTypes
        );

        List<FilterIndexDocument> filters = jdbcTemplate.query(GET_FILTERS_BY_RESOURCE_ID_QUERY, setQueryParameters(id.toString()),
                this::extractFilters
        );

        List<ParentTopicIndexDocument> topics = jdbcTemplate.query(GET_TOPICS_FOR_RESOURCE, setQueryParameters(language, id.toString()),
                this::extractParentTopics);

        List<String> paths = jdbcTemplate.query("SELECT path FROM cached_url WHERE public_id = ?", setQueryParameters(id.toString()),
                resultSet -> {
                    List<String> res = new ArrayList<>();
                    while (resultSet.next()) {
                        res.add(resultSet.getString("path"));
                    }
                    return res;
                });


        ResourceFullIndexDocument r = ResourceFullIndexDocument.from(resource);
        r.resourceTypes.addAll(resourceTypes);
        r.filters.addAll(filters);
        r.parentTopics.addAll(topics);
        r.paths.addAll(paths);
        return r;
    }


    private ResourceWithPathsIndexDocument extractResourceWithPaths(ResultSet resultSet) throws SQLException {
        ResourceWithPathsIndexDocument doc = null;
        while (resultSet.next()) {
            String path = resultSet.getString("resource_path");
            boolean primary = resultSet.getBoolean("path_is_primary");
            if (doc == null) {
                doc = new ResourceWithPathsIndexDocument();
                doc.id = getURI(resultSet, "resource_public_id");
                doc.contentUri = getURI(resultSet, "resource_content_uri");
                doc.name = resultSet.getString("resource_name");
                doc.paths = new ArrayList<>();
            }
            if (primary && (doc.path == null || !doc.path.startsWith("/topic"))) {
                doc.path = path;
            }
            doc.paths.add(path);
        }
        if (doc != null) {
            Collections.sort(doc.paths);
        }
        return doc;
    }

    private List<ParentTopicIndexDocument> extractParentTopics(ResultSet resultSet) throws SQLException {
        List<ParentTopicIndexDocument> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(new ParentTopicIndexDocument() {{
                name = resultSet.getString("name");
                id = getURI(resultSet, "id");
                isPrimary = resultSet.getBoolean("is_primary");
                contentUri = URI.create(resultSet.getString("content_uri") != null ? resultSet.getString("content_uri") : "");
                connectionId = URI.create(resultSet.getString("connection_id"));
            }});
        }
        return result;
    }


    private List<ResourceTypeIndexDocument> extractResourceTypes(ResultSet resultSet) throws SQLException {
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


    private List<FilterIndexDocument> extractFilters(ResultSet resultSet) throws SQLException {
        List<FilterIndexDocument> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(new FilterIndexDocument() {{
                name = resultSet.getString("filter_name");
                id = getURI(resultSet, "filter_public_id");
                connectionId = getURI(resultSet, "resource_filter_public_id");
                relevanceId = getURI(resultSet, "relevance_id");
            }});
        }
        return result;
    }

}
