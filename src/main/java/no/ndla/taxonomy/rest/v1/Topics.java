package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.service.repositories.TopicRepository;
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
import static no.ndla.taxonomy.rest.v1.UrlResolver.getPathMostCloselyMatchingContext;

@RestController
@RequestMapping(path = {"/v1/topics"})
@Transactional
public class Topics extends CrudController<Topic> {

    private JdbcTemplate jdbcTemplate;

    private static final String GET_TOPICS_QUERY = getQuery("get_topics");
    private static final String GET_RESOURCES_BY_TOPIC_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_resources_by_topic_public_id_recursively");
    private static final String GET_RESOURCES_BY_TOPIC_PUBLIC_ID_QUERY = getQuery("get_resources_by_topic_public_id");
    private static final String GET_FILTERS_BY_TOPIC_ID_QUERY = getQuery("get_filters_by_topic_public_id");
    private static final String GET_SUBTOPICS_BY_TOPIC_ID_QUERY = getQuery("get_subtopics_by_topic_id_query");
    private static final String GET_SUBJECT_CONNECTIONS_BY_TOPIC_ID_QUERY = getQuery("get_subject_connections_by_topic_id");
    private static final String GET_SUBTOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY = getQuery("get_subtopic_connections_by_topic_id");


    public Topics(TopicRepository topicRepository, JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        repository = topicRepository;
    }

    @GetMapping
    @ApiOperation("Gets all topics")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<TopicIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) throws Exception {
        List<Object> args = asList(language);
        TopicQueryExtractor extractor = new TopicQueryExtractor();
        return jdbcTemplate.query(GET_TOPICS_QUERY, setQueryParameters(args),
                extractor::extractTopics
        );
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    @PreAuthorize("hasAuthority('READONLY')")
    public TopicIndexDocument get(@PathVariable("id") URI id,
                                  @ApiParam(value = LANGUAGE_DOC, example = "nb")
                                  @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        String sql = GET_TOPICS_QUERY.replace("1 = 1", "t.public_id = ?");
        List<Object> args = asList(language, id.toString());

        TopicQueryExtractor extractor = new TopicQueryExtractor();
        return getFirst(jdbcTemplate.query(sql, setQueryParameters(args),
                extractor::extractTopics
        ), "Topic", id);
    }

    @PostMapping
    @ApiOperation(value = "Creates a new topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "connection", value = "The new topic") @RequestBody CreateTopicCommand command) {
        return doPost(new Topic(), command);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a single topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "topic", value = "The updated topic. Fields not included will be set to null.") @RequestBody UpdateTopicCommand command) {
        doPut(id, command);
    }

    @GetMapping("/{id}/resources")
    @PreAuthorize("hasAuthority('READONLY')")
    @ApiOperation(value = "Gets all resources for the given topic")
    public List<ResourceIndexDocument> getResources(
            @PathVariable("id") URI topicId,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
            @ApiParam("If true, resources from subtopics are fetched recursively")
                    boolean recursive,
            @RequestParam(value = "type", required = false, defaultValue = "")
            @ApiParam(value = "Select by resource type id(s). If not specified, resources of all types will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] resourceTypeIds,
            @RequestParam(value = "filter", required = false, defaultValue = "")
            @ApiParam(value = "Select by filter id(s). If not specified, all resources will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false, defaultValue = "")
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance
    ) throws Exception {

        TopicIndexDocument topicIndexDocument = get(topicId, null);

        List<Object> args = new ArrayList<>();
        String query;
        if (recursive) {
            query = GET_RESOURCES_BY_TOPIC_PUBLIC_ID_RECURSIVELY_QUERY;
            args.add(topicId.toString());
            args.add(language);
            args.add(language);
        } else {
            query = GET_RESOURCES_BY_TOPIC_PUBLIC_ID_QUERY;
            args.add(language);
            args.add(language);
            args.add(topicId.toString());
        }
        ResourceQueryExtractor extractor = new ResourceQueryExtractor();
        query = extractor.addResourceTypesToQuery(resourceTypeIds, args, query);
        query = extractor.addFiltersToQuery(filterIds, args, query);

        return jdbcTemplate.query(query, setQueryParameters(args), resultSet -> {
            return extractor.extractResources(relevance, topicIndexDocument, resultSet);
        });
    }


    @GetMapping("/{id}/filters")
    @PreAuthorize("hasAuthority('READONLY')")
    @ApiOperation(value = "Gets all filters associated with this topic")
    public List<FilterIndexDocument> getFilters(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        FilterQueryExtractor extractor = new FilterQueryExtractor();
        return jdbcTemplate.query(GET_FILTERS_BY_TOPIC_ID_QUERY, setQueryParameters(singletonList(id.toString())),
                extractor::extractFilters
        );
    }

    @GetMapping("/{id}/topics")
    @PreAuthorize("hasAuthority('READONLY')")
    @ApiOperation(value = "Gets all subtopics for this topic")
    public List<SubTopicIndexDocument> getSubTopics(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        String sql = GET_SUBTOPICS_BY_TOPIC_ID_QUERY.replace("1 = 1", "t.public_id = ?");
        List<Object> args = asList(language, id.toString());

        SubTopicQueryExtractor extractor = new SubTopicQueryExtractor();
        return jdbcTemplate.query(sql, setQueryParameters(args),
                extractor::extractSubTopics
        );
    }

    @GetMapping("/{id}/connections")
    @PreAuthorize("hasAuthority('READONLY')")
    @ApiOperation(value = "Gets all subjects and subtopics this topic is connected to")
    public List<ConnectionIndexDocument> getAllConnections(@PathVariable("id") URI id) {
        String subtopicQuery = GET_SUBTOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY.replace("1 = 1", "t.public_id = ?");
        String subjectTopicQuery = GET_SUBJECT_CONNECTIONS_BY_TOPIC_ID_QUERY.replace("1 = 1", "t.public_id = ?");
        List<Object> args = asList(id.toString());
        List<ConnectionIndexDocument> results = new ArrayList<>();
        ConnectionQueryExctractor connectionQueryExctractor = new ConnectionQueryExctractor();
        results.addAll(jdbcTemplate.query(subjectTopicQuery, setQueryParameters(args), connectionQueryExctractor::extractConnections));
        results.addAll(jdbcTemplate.query(subtopicQuery, setQueryParameters(args), connectionQueryExctractor::extractConnections));
        return results;
    }

    @ApiModel("TopicResourceIndexDocument")
    public static class ResourceIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:123")
        public URI topicId;

        @JsonProperty
        @ApiModelProperty(value = "Resource id", example = "urn:resource:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Resource name", example = "Basic physics")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "Resource type(s)", example = "[{id = 'urn:resourcetype:1', name = 'lecture'}]")
        public Set<ResourceTypeIndexDocument> resourceTypes = new HashSet<>();

        @JsonProperty
        @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
                notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                        "for the system, and <id> is the id of this content in that system.",
                example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "URL path for resource", example = "'/subject:1/topic:12/resource:12'")
        public String path;

        @JsonProperty
        @ApiModelProperty(value = "The id of the topic-resource connection which causes this resource to be included in the result set.", example = "urn:topic-resource:1")
        public URI connectionId;

        @JsonProperty
        @ApiModelProperty(value = "The order in which to sort the topic within it's level.", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
        public Boolean isPrimary;
    }

    @ApiModel("TopicResourceTypeIndexDocument")
    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Resource type id", example = "urn:resourcetype:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Resource type name", example = "Assignment")
        public String name;

        @Override
        @JsonIgnore
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ResourceTypeIndexDocument)) return false;

            ResourceTypeIndexDocument that = (ResourceTypeIndexDocument) o;

            return id.equals(that.id);
        }

        @Override
        @JsonIgnore
        public int hashCode() {
            return id.hashCode();
        }
    }

    public static class CreateTopicCommand extends CreateCommand<Topic> {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:topic:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the topic", example = "Trigonometry")
        public String name;

        @Override
        public URI getId() {
            return id;
        }

        @Override
        public void apply(Topic topic) {
            topic.setName(name);
            topic.setContentUri(contentUri);
        }
    }

    public static class UpdateTopicCommand extends UpdateCommand<Topic> {
        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the topic", example = "Trigonometry")
        public String name;

        @Override
        public void apply(Topic topic) {
            topic.setName(name);
            topic.setContentUri(contentUri);
        }
    }

    @ApiModel("TopicIndexDocument")
    public static class TopicIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the topic", example = "Trigonometry")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "The path part of the url for this topic", example = "/subject:1/topic:1")
        public String path;

        TopicIndexDocument() {
        }
    }

    @ApiModel("SubTopicIndexDocument")
    public static class SubTopicIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the subtopic", example = "Trigonometry")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
        public Boolean isPrimary;

        SubTopicIndexDocument() {
        }
    }

    @ApiModel("Topic FilterIndexDocument")
    public static class FilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:filter:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the filter", example = "1T-YF")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "The id of the filter topic connection", example = "urn:topic-filter:1")
        public URI connectionId;

        @JsonProperty
        @ApiModelProperty(value = "The relevance of this topic according to the filter", example = "urn:relevance:core")
        public URI relevanceId;
    }

    @ApiModel("Connections")
    public static class ConnectionIndexDocument {

        @JsonProperty
        @ApiModelProperty(value = "The id of the subject-topic or topic-subtopic connection", example = "urn:subject-topic:1")
        public URI connectionId;
        @JsonProperty
        @ApiModelProperty(value = "The id of the connected subject or topic", example = "urn:subject-topic:1")
        public URI targetId;
        @JsonProperty
        @ApiModelProperty(value = "The path part of the url for the subject or subtopic connected to this topic", example = "/subject:1/topic:1")
        public String path;
        @JsonProperty
        @ApiModelProperty(value = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
        public Boolean isPrimary;
    }

    private class TopicQueryExtractor {

        private List<TopicIndexDocument> extractTopics(ResultSet resultSet) throws SQLException {
            List<TopicIndexDocument> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new TopicIndexDocument() {{
                    name = resultSet.getString("topic_name");
                    id = getURI(resultSet, "topic_public_id");
                    contentUri = getURI(resultSet, "topic_content_uri");
                    path = resultSet.getString("topic_path");
                }});
            }
            return result;
        }
    }

    private class ResourceQueryExtractor {

        String addFiltersToQuery(URI[] filterIds, List<Object> args, String query) {
            if (filterIds.length > 0) {
                StringBuilder where = new StringBuilder();
                for (URI filterId : filterIds) {
                    where.append("f.public_id = ? OR ");
                    args.add(filterId.toString());
                }
                where.setLength(where.length() - 4);
                query = query.replace("2 = 2", "(" + where + ")");
            }
            return query;
        }

        String addResourceTypesToQuery(URI[] resourceTypeIds, List<Object> args, String query) {
            if (resourceTypeIds.length > 0) {
                StringBuilder where = new StringBuilder();
                for (URI resourceTypeId : resourceTypeIds) {
                    where.append("rt.public_id = ? OR ");
                    args.add(resourceTypeId.toString());
                }
                where.setLength(where.length() - 4);
                query = query.replace("1 = 1", "(" + where + ")");
            }
            return query;
        }

        List<ResourceIndexDocument> extractResources(@RequestParam(value = "relevance", required = false, defaultValue = "") @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.") URI relevance, TopicIndexDocument topicIndexDocument, ResultSet resultSet) throws SQLException {
            List<ResourceIndexDocument> result = new ArrayList<>();
            Map<URI, ResourceIndexDocument> resources = new HashMap<>();

            String context = topicIndexDocument.path;

            while (resultSet.next()) {
                URI id = toURI(resultSet.getString("resource_public_id"));

                ResourceIndexDocument resource = resources.get(id);
                if (null == resource) {
                    resource = new ResourceIndexDocument() {{
                        topicId = toURI(resultSet.getString("topic_id"));
                        name = resultSet.getString("resource_name");
                        contentUri = toURI(resultSet.getString("resource_content_uri"));
                        id = toURI(resultSet.getString("resource_public_id"));
                        connectionId = toURI(resultSet.getString("connection_public_id"));
                        rank = resultSet.getInt("rank");
                        isPrimary = resultSet.getBoolean("resource_is_primary");
                    }};
                    resources.put(id, resource);
                    filterResultByRelevance(relevance, resultSet, result, resource);
                }
                resource.path = getPathMostCloselyMatchingContext(context, resource.path, resultSet.getString("resource_path"));

                String resourceTypePublicId = resultSet.getString("resource_type_public_id");
                if (resourceTypePublicId != null) {
                    ResourceTypeIndexDocument resourceType = new ResourceTypeIndexDocument() {{
                        id = toURI(resourceTypePublicId);
                        name = resultSet.getString("resource_type_name");
                    }};

                    resource.resourceTypes.add(resourceType);
                }
            }

            return result;
        }

        private void filterResultByRelevance(URI relevance, ResultSet resultSet, List<ResourceIndexDocument> result, ResourceIndexDocument resource) throws SQLException {
            if (relevance == null || relevance.toString().equals("")) {
                result.add(resource);
            } else {
                URI resourceRelevance = toURI(resultSet.getString("relevance_public_id"));
                if (resourceRelevance != null && resourceRelevance.equals(relevance)) {
                    result.add(resource);
                }
            }
        }
    }

    private class FilterQueryExtractor {

        private List<FilterIndexDocument> extractFilters(ResultSet resultSet) throws SQLException {
            List<FilterIndexDocument> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new FilterIndexDocument() {{
                    name = resultSet.getString("filter_name");
                    id = getURI(resultSet, "filter_public_id");
                    connectionId = getURI(resultSet, "topic_filter_public_id");
                    relevanceId = getURI(resultSet, "relevance_id");
                }});
            }
            return result;
        }
    }

    private class SubTopicQueryExtractor {

        private List<SubTopicIndexDocument> extractSubTopics(ResultSet resultSet) throws SQLException {
            List<SubTopicIndexDocument> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new SubTopicIndexDocument() {{
                    name = resultSet.getString("subtopic_name");
                    id = getURI(resultSet, "subtopic_public_id");
                    contentUri = getURI(resultSet, "subtopic_content_uri");
                    isPrimary = resultSet.getBoolean("subtopic_is_primary");
                }});
            }
            return result;
        }
    }

    private class ConnectionQueryExctractor {
        private List<ConnectionIndexDocument> extractConnections(ResultSet resultSet) throws SQLException {
            List<ConnectionIndexDocument> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new ConnectionIndexDocument() {{
                    connectionId = getURI(resultSet, "connection_id");
                    isPrimary = resultSet.getBoolean("is_primary");
                    targetId = getURI(resultSet, "target_id");
                    path = resultSet.getString("path");
                }});

            }
            return result;
        }

    }
}
