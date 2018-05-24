package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.repositories.ResourceRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"/v1/resources"})
@Transactional
public class Resources extends CrudController<Resource> {

    private static final String GET_RESOURCES_QUERY = getQuery("get_resources");
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
    @PreAuthorize("hasAuthority('READONLY')")
    public List<ResourceIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        return getResourceIndexDocuments(GET_RESOURCES_QUERY, singletonList(language));
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single resource")
    @PreAuthorize("hasAuthority('READONLY')")
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
    @RequestBody UpdateResourceCommand command) throws Exception {
        doPut(id, command);
    }

    @PostMapping
    @ApiOperation(value = "Adds a new resource")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "resource", value = "the new resource") @RequestBody CreateResourceCommand command) throws Exception {
        return doPost(new Resource(), command);
    }

    @GetMapping("/{id}/resource-types")
    @ApiOperation(value = "Gets all resource types associated with this resource")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<ResourceTypeIndexDocument> getResourceTypes(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        ResourceTypeQueryExtractor extractor = new ResourceTypeQueryExtractor();
        return jdbcTemplate.query(GET_RESOURCE_RESOURCE_TYPES_QUERY, setQueryParameters(asList(language, id.toString())),
                extractor::extractResourceTypes
        );
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters associated with this resource")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<FilterIndexDocument> getFilters(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        FilterQueryExtractor extractor = new FilterQueryExtractor();
        return jdbcTemplate.query(GET_FILTERS_BY_RESOURCE_ID_QUERY, setQueryParameters(singletonList(id.toString())),
                resultSet -> {
                    return extractor.extractFilters(resultSet);
                }
        );
    }

    @GetMapping("/{id}/full")
    @ApiOperation(value = "Gets all topics, filters and resourceTypes for this resource")
    @PreAuthorize("hasAuthority('READONLY')")
    public ResourceFullIndexDocument getResourceFull(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        String sql = GET_RESOURCES_QUERY.replace("1 = 1", "r.public_id = ?");
        List<Object> args = asList(language, id.toString());

        ResourceIndexDocument resource = getFirst(getResourceIndexDocuments(sql, args), "Resource", id);
        TopicQueryExtractor extractor = new TopicQueryExtractor();

        List<TopicAndPrimaryIndexDocument> topics = jdbcTemplate.query(GET_TOPICS_FOR_RESOURCE, setQueryParameters(asList("nb", id.toString())), resultSet ->
        {
            return extractor.extractTopics(resultSet);
        });

        ResourceFullIndexDocument result = new ResourceFullIndexDocument();
        return result;
    }

    class TopicQueryExtractor {
        private List<TopicAndPrimaryIndexDocument> extractTopics(ResultSet resultSet) throws SQLException {
            List<TopicAndPrimaryIndexDocument> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new TopicAndPrimaryIndexDocument() {{
                    name = resultSet.getString("name");
                    id = getURI(resultSet, "id");
                }});
            }
            return result;
        }
    }

    public static class CreateResourceCommand extends CreateCommand<Resource> {
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

        @Override
        public URI getId() {
            return id;
        }

        @Override
        public void apply(Resource entity) {
            entity.setName(name);
            entity.setContentUri(contentUri);
        }
    }

    static class UpdateResourceCommand extends UpdateCommand<Resource> {
        @JsonProperty
        @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
                notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                        "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "The name of the resource", example = "Introduction to integration")
        public String name;

        @Override
        public void apply(Resource resource) {
            resource.setName(name);
            resource.setContentUri(contentUri);
        }
    }

    @ApiModel("ResourceIndexDocument")
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

    @ApiModel("ResourceFullIndexDocument")
    static class ResourceFullIndexDocument extends ResourceIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Owning topology nodes and whether or not connection type is primary", example = "[{id = 'urn:resourcetype:1', name = 'lecture'}]")
        public Set<TopicAndPrimaryIndexDocument> topics = new HashSet<>();

        @JsonProperty
        @ApiModelProperty(value = "Resource type(s)", example = "[{id = 'urn:resourcetype:1', name = 'lecture'}]")
        public Set<Subjects.ResourceTypeIndexDocument> resourceTypes = new HashSet<>();
    }

    @ApiModel("TopicAndPrimaryIndexDocument")
    static class TopicAndPrimaryIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "The primary path part of the url to this topic.", example = "/subject:1/topic:1")
        public String path;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean isPrimary;

        @Override
        @JsonIgnore
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Subjects.TopicIndexDocument)) return false;

            Subjects.TopicIndexDocument that = (Subjects.TopicIndexDocument) o;

            return id.equals(that.id);
        }

        @Override
        @JsonIgnore
        public int hashCode() {
            return id.hashCode();
        }
    }

    @ApiModel("ConnectionResourceResourceTypeIndexDocument")
    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:resourcetype:2")
        public URI id;

        @JsonProperty
        @ApiModelProperty(example = "urn:resourcetype:1")
        public URI parentId;

        @JsonProperty
        @ApiModelProperty(value = "The name of the resource type", example = "Lecture")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "The id of the resource resource type connection", example = "urn:resource-resourcetype:1")
        public URI connectionId;
    }

    @ApiModel("ResourceFilterIndexDocument")
    public static class FilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:filter:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the filter", example = "1T-YF")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "The id of the filter resource connection", example = "urn:resource-filter:1")
        public URI connectionId;

        @JsonProperty
        @ApiModelProperty(value = "The relevance of this resource according to the filter", example = "urn:relevance:1")
        public URI relevanceId;
    }

    private class ResourceTypeQueryExtractor {
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

    }

    private class FilterQueryExtractor {
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
}
