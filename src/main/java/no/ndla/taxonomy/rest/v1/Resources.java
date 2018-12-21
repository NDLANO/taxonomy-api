package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.ndla.taxonomy.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.rest.v1.DocStrings.LANGUAGE_DOC;

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
    ) {
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
    ) {
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
    @PreAuthorize("hasAuthority('READONLY')")
    public List<ResourceTypeIndexDocument> getResourceTypes(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
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
    ) {
        FilterQueryExtractor extractor = new FilterQueryExtractor();
        return jdbcTemplate.query(GET_FILTERS_BY_RESOURCE_ID_QUERY, setQueryParameters(singletonList(id.toString())),
                resultSet -> {
                    return extractor.extractFilters(resultSet);
                }
        );
    }

    @GetMapping("/{id}/full")
    @ApiOperation(value = "Gets all parent topics, all filters and resourceTypes for this resource")
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

        ResourceTypeQueryExtractor resourceTypeQueryExtractor = new ResourceTypeQueryExtractor();
        List<ResourceTypeIndexDocument> resourceTypes = jdbcTemplate.query(GET_RESOURCE_RESOURCE_TYPES_QUERY, setQueryParameters(asList(language, id.toString())),
                resultSet -> {
                    return resourceTypeQueryExtractor.extractResourceTypes(resultSet);
                }
        );

        FilterQueryExtractor filterQueryExtractor = new FilterQueryExtractor();
        List<FilterIndexDocument> filters = jdbcTemplate.query(GET_FILTERS_BY_RESOURCE_ID_QUERY, setQueryParameters(singletonList(id.toString())),
                resultSet -> {
                    return filterQueryExtractor.extractFilters(resultSet);
                }
        );

        TopicQueryExtractor topicQueryExtractor = new TopicQueryExtractor();
        List<ParentTopicIndexDocument> topics = jdbcTemplate.query(GET_TOPICS_FOR_RESOURCE, setQueryParameters(asList(language, id.toString())),
                resultSet ->
                {
                    return topicQueryExtractor.extractTopics(resultSet);
                });

        ResourceFullIndexDocument r = ResourceFullIndexDocument.from(resource);
        r.resourceTypes.addAll(resourceTypes);
        r.filters.addAll(filters);
        r.parentTopics.addAll(topics);
        return r;
    }

    class TopicQueryExtractor {
        private List<ParentTopicIndexDocument> extractTopics(ResultSet resultSet) throws SQLException {
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
        @ApiModelProperty(value = "Related resource type(s)", example = "[" +
                "{\"id\": \"urn:resourcetype:learningPath\"," +
                " \"name\": \"Læringssti\"}]")
        public Set<ResourceTypeIndexDocument> resourceTypes = new HashSet<>();

        @JsonProperty
        @ApiModelProperty(value = "Filters", example = "[" +
                "{\"id\": \"urn:filter:047bb226-48d1-4122-8791-7d4f5d83cf8b\"," +
                "\"name\": \"VG2\"," +
                "\"connectionId\": \"urn:resource-filter:a41d6162-b67f-44d8-b440-c1fdc7b4d05e\"," +
                "\"relevanceId\": \"urn:relevance:core\"}]")
        public Set<FilterIndexDocument> filters = new HashSet<>();

        @JsonProperty
        @ApiModelProperty(value = "Parent topology nodes and whether or not connection type is primary",
                example = "["+
                "{\"id\": \"urn:topic:1:181900\"," +
                        "\"name\": \"I dyrehagen\"," +
                        "\"contentUri\": \"urn:article:6662\"," +
                        "\"path\": \"/subject:2/topic:1:181900\"," +
                        "\"primary\": \"true\"}]")
        public Set<ParentTopicIndexDocument> parentTopics = new HashSet<>();

        static ResourceFullIndexDocument from(ResourceIndexDocument resource) {
            ResourceFullIndexDocument r = new ResourceFullIndexDocument();
            r.id = resource.id;
            r.name = resource.name;
            r.contentUri = resource.contentUri;
            r.path = resource.path;
            return r;
        }
    }

    @ApiModel("ParentTopicIndexDocument")
    static class ParentTopicIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean isPrimary;

        @JsonProperty
        public URI connectionId;

        @Override
        @JsonIgnore
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ParentTopicIndexDocument that = (ParentTopicIndexDocument) o;
            return Objects.equals(id, that.id);
        }

        @Override
        @JsonIgnore
        public int hashCode() {
            return id.hashCode();
        }
    }

    @ApiModel("ResourceTypeIndexDocument")
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

        @Override
        @JsonIgnore
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResourceTypeIndexDocument that = (ResourceTypeIndexDocument) o;
            return Objects.equals(id, that.id);
        }

        @Override
        @JsonIgnore
        public int hashCode() {
            return id.hashCode();
        }
    }

    @ApiModel("FilterIndexDocument")
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

        @JsonIgnore
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilterIndexDocument that = (FilterIndexDocument) o;
            return Objects.equals(id, that.id);
        }

        @JsonIgnore
        @Override
        public int hashCode() {
            return id.hashCode();
        }
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
