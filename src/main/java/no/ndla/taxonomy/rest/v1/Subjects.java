package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.repositories.SubjectRepository;
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
@Transactional
@RequestMapping(path = {"/v1/subjects"})
public class Subjects extends CrudController<Subject> {
    private static final String GET_SUBJECTS_QUERY = getQuery("get_subjects");
    private static final String GET_RESOURCES_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_resources_by_subject_public_id_recursively");
    private static final String GET_TOPICS_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_topics_by_subject_public_id_recursively");
    private static final String GET_FILTERS_BY_SUBJECT_PUBLIC_ID_QUERY = getQuery("get_filters_by_subject_public_id");

    private SubjectRepository subjectRepository;
    private JdbcTemplate jdbcTemplate;

    public Subjects(SubjectRepository subjectRepository, JdbcTemplate jdbcTemplate) {
        this.subjectRepository = subjectRepository;
        this.jdbcTemplate = jdbcTemplate;
        repository = subjectRepository;
    }

    @GetMapping
    @ApiOperation("Gets all subjects")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<SubjectIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        List<Object> args = singletonList(language);
        SubjectQueryExtractor extractor = new SubjectQueryExtractor();
        return jdbcTemplate.query(GET_SUBJECTS_QUERY, setQueryParameters(args), extractor::extractSubjects);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single subject", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    @PreAuthorize("hasAuthority('READONLY')")
    public SubjectIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        String sql = GET_SUBJECTS_QUERY.replace("1 = 1", "s.public_id = ?");
        List<Object> args = asList(language, id.toString());

        SubjectQueryExtractor extractor = new SubjectQueryExtractor();
        return getFirst(jdbcTemplate.query(sql, setQueryParameters(args), extractor::extractSubjects), "Subject", id);
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a subject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "subject", value = "The updated subject. Fields not included will be set to null.") @RequestBody UpdateSubjectCommand command
    ) {
        doPut(id, command);
    }

    @PostMapping
    @ApiOperation(value = "Creates a new subject")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "subject", value = "The new subject") @RequestBody CreateSubjectCommand command) {
        return doPost(new Subject(), command);
    }

    @GetMapping("/{id}/topics")
    @ApiOperation(value = "Gets all topics associated with a subject", notes = "This resource is read-only. To update the relationship between subjects and topics, use the resource /subject-topics.")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<SubTopicIndexDocument> getTopics(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
            @ApiParam("If true, subtopics are fetched recursively")
                    boolean recursive,
            @RequestParam(value = "filter", required = false, defaultValue = "")
            @ApiParam(value = "Select by filter id(s). If not specified, all topics will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false, defaultValue = "")
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance
    ) {
        String sql = GET_TOPICS_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY;
        if (!recursive) sql = sql.replace("1 = 1", "t.level = 0");

        List<Object> args = new ArrayList<>();
        args.add(id.toString());
        args.add(language);

        TopicQueryExtractor extractor = new TopicQueryExtractor();
        return jdbcTemplate.query(sql, setQueryParameters(args), resultSet -> {
            return extractor.extractTopics(id, filterIds, relevance, resultSet);
        });
    }

    @GetMapping("/{id}/resources")
    @ApiOperation(value = "Gets all resources for a subject (all topics and subtopics)")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<ResourceIndexDocument> getResources(
            @PathVariable("id") URI subjectId,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "type", required = false, defaultValue = "")
            @ApiParam(value = "Filter by resource type id(s). If not specified, resources of all types will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] resourceTypeIds,
            @RequestParam(value = "filter", required = false, defaultValue = "")
            @ApiParam(value = "Select by filter id(s). If not specified, all resources will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false, defaultValue = "")
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance
    ) {
        List<Object> args = new ArrayList<>();
        args.add(subjectId.toString());
        args.add(language);
        args.add(language);

        String sql = GET_RESOURCES_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY;
        ResourceQueryExtractor extractor = new ResourceQueryExtractor();
        sql = extractor.addResourceTypesToQuery(resourceTypeIds, sql, args);
        sql = extractor.addFilterToQuery(filterIds, sql, args);

        return jdbcTemplate.query(sql, setQueryParameters(args), resultSet -> {
            return extractor.extractResources(subjectId, relevance, resultSet);
        });
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters for a subject")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<FilterIndexDocument> getFilters(@PathVariable("id") URI subjectId) {
        String sql = GET_FILTERS_BY_SUBJECT_PUBLIC_ID_QUERY;
        List<Object> args = singletonList(subjectId.toString());

        FilterQueryExtractor extractor = new FilterQueryExtractor();
        return jdbcTemplate.query(sql, setQueryParameters(args), extractor::extractFilters);
    }


    public static class CreateSubjectCommand extends CreateCommand<Subject> {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:subject:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the subject", example = "Mathematics")
        public String name;

        @Override
        public URI getId() {
            return id;
        }

        @Override
        public void apply(Subject subject) {
            subject.setName(name);
            subject.setContentUri(contentUri);
        }
    }

    public static class UpdateSubjectCommand extends UpdateCommand<Subject> {
        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the subject", example = "Mathematics")
        public String name;

        @Override
        public void apply(Subject subject) {
            subject.setName(name);
            subject.setContentUri(contentUri);
        }
    }

    @ApiModel("SubjectIndexDocument")
    public static class SubjectIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:subject:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "The name of the subject", example = "Mathematics")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "The path part of the url to this subject.", example = "/subject:1")
        public String path;
    }

    @ApiModel("SubjectTopicIndexDocument")
    public static class SubTopicIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty("Parent id in the current context, null if none exists")
        public URI parent;

        @JsonProperty
        @ApiModelProperty(value = "The primary path to this topic.", example = "/subject:1/topic:1")
        public String path;

        @JsonProperty
        @ApiModelProperty(value = "The id of the subject-topics or topic-subtopics connection which causes this topic to be included in the result set.", example = "urn:subject-topic:1")
        public URI connectionId;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean isPrimary;

        @JsonProperty
        @ApiModelProperty(value = "The order in which to sort the topic within it's level.", example = "1")
        public int rank;

        @JsonProperty
        @ApiModelProperty(value = "Filters this topic is associated with, directly or by inheritance", example = "[{id = 'urn:filter:1', relevanceId='urn:relevance:core'}]")
        public Set<TopicFilterIndexDocument> filters = new HashSet<>();

        @JsonIgnore
        URI topicFilterId, resourceFilterId, filterPublicId;

        @Override
        @JsonIgnore
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SubTopicIndexDocument)) return false;

            SubTopicIndexDocument that = (SubTopicIndexDocument) o;

            return id.equals(that.id);
        }

        @Override
        @JsonIgnore
        public int hashCode() {
            return id.hashCode();
        }
    }

    @ApiModel("SubjectTopicFilterIndexDocument")
    public static class TopicFilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Filter id", example = "urn:filter:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Filter name", example = "VG 1")
        public String name;

        @JsonProperty
        @ApiModelProperty(required = true, value = "ID of the relevance the resource has in context of the filter", example = "urn:relevance:core")
        public URI relevanceId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TopicFilterIndexDocument that = (TopicFilterIndexDocument) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {

            return Objects.hash(id.toASCIIString());
        }
    }

    @ApiModel("SubjectResourceIndexDocument")
    public static class ResourceIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:12")
        public URI topicId;

        @JsonProperty
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
        @ApiModelProperty(value = "URL path for resource", example = "'/subject:1/topic:12/resource:12")
        public String path;

        @JsonProperty
        @ApiModelProperty(value = "The id of the topic-resource connection which causes this resource to be included in the result set.", example = "urn:topic-resource:1")
        public URI connectionId;

        @JsonProperty
        @ApiModelProperty(value = "Filters this resource is associated with, directly or by inheritance", example = "[{id = 'urn:filter:1', relevanceId='urn:relevance:core'}]")
        public Set<ResourceFilterIndexDocument> filters = new HashSet<>();
    }

    @ApiModel("SubjectResourceTypeIndexDocument")
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

    @ApiModel("SubjectFilterIndexDocument")
    public static class FilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Filter id", example = "urn:filter:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Filter name", example = "1T-YF")
        public String name;
    }

    @ApiModel("SubjectResourceFilterIndexDocument")
    public static class ResourceFilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Filter id", example = "urn:filter:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "ID of the relevance the resource has in context of the filter", example = "urn:relevance:core")
        public URI relevanceId;
    }

    class ResourceQueryExtractor {
        String addFilterToQuery(URI[] filterIds, String sql, List<Object> args) {
            if (filterIds.length > 0) {
                StringBuilder where = new StringBuilder();
                for (URI filterId : filterIds) {
                    where.append("f.public_id = ? OR ");
                    args.add(filterId.toString());
                }
                where.setLength(where.length() - 4);
                sql = sql.replace("2 = 2", "(" + where + ")");
            }
            return sql;
        }

        String addResourceTypesToQuery(URI[] resourceTypeIds, String sql, List<Object> args) {
            if (resourceTypeIds.length > 0) {
                StringBuilder where = new StringBuilder();
                for (URI resourceTypeId : resourceTypeIds) {
                    where.append("rt.public_id = ? OR ");
                    args.add(resourceTypeId.toString());
                }
                where.setLength(where.length() - " OR ".length());
                sql = sql.replace("1 = 1", "(" + where + ")");
            }
            return sql;
        }

        List<ResourceIndexDocument> extractResources(URI subjectId, URI relevance, ResultSet resultSet) throws SQLException {
            List<ResourceIndexDocument> result = new ArrayList<>();
            Map<URI, ResourceIndexDocument> resources = new HashMap<>();

            String context = "/" + subjectId.toString().substring(4);

            while (resultSet.next()) {
                URI id = toURI(resultSet.getString("resource_public_id"));

                ResourceIndexDocument resource = extractResource(relevance, resultSet, result, resources, id);
                resource.path = getPathMostCloselyMatchingContext(context, resource.path, resultSet.getString("resource_path"));
                extractResourceType(resultSet, resource);
                extractFilter(resultSet, resource);
            }
            return result;
        }

        private void extractFilter(ResultSet resultSet, ResourceIndexDocument resource) throws SQLException {
            URI filterPublicId = getURI(resultSet, "filter_public_id");
            if (null != filterPublicId) {
                ResourceFilterIndexDocument filter = new ResourceFilterIndexDocument() {{
                    id = filterPublicId;
                    relevanceId = getURI(resultSet, "relevance_public_id");
                }};

                resource.filters.add(filter);
            }
        }

        private void extractResourceType(ResultSet resultSet, ResourceIndexDocument resource) throws SQLException {
            URI resource_type_id = getURI(resultSet, "resource_type_public_id");
            if (resource_type_id != null) {
                ResourceTypeIndexDocument resourceType = new ResourceTypeIndexDocument() {{
                    id = resource_type_id;
                    name = resultSet.getString("resource_type_name");
                }};

                resource.resourceTypes.add(resourceType);
            }
        }

        private ResourceIndexDocument extractResource(URI relevance, ResultSet resultSet, List<ResourceIndexDocument> result, Map<URI, ResourceIndexDocument> resources, URI id) throws SQLException {
            ResourceIndexDocument resource = resources.get(id);
            if (null == resource) {
                resource = new ResourceIndexDocument() {{
                    topicId = toURI(resultSet.getString("topic_public_id"));
                    contentUri = toURI(resultSet.getString("resource_content_uri"));
                    name = resultSet.getString("resource_name");
                    id = toURI(resultSet.getString("resource_public_id"));
                    connectionId = toURI(resultSet.getString("connection_public_id"));
                }};
                resources.put(id, resource);
                filterResourceByRelevance(relevance, resultSet, result, resource);
            }
            return resource;
        }

        private void filterResourceByRelevance(URI relevance, ResultSet resultSet, List<ResourceIndexDocument> result, ResourceIndexDocument resource) throws SQLException {
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

    class TopicQueryExtractor {
        List<SubTopicIndexDocument> extractTopics(URI id, URI[] filterIds, URI relevance, ResultSet resultSet) throws SQLException {
            Map<URI, SubTopicIndexDocument> topics = new HashMap<>();
            List<SubTopicIndexDocument> queryresult = new ArrayList<>();
            String context = "/" + id.toString().substring(4);
            while (resultSet.next()) {
                URI public_id = getURI(resultSet, "public_id");

                SubTopicIndexDocument topic = extractTopic(relevance, resultSet, topics, queryresult, public_id);
                topic.path = getPathMostCloselyMatchingContext(context, topic.path, resultSet.getString("topic_path"));
                extractFilter(resultSet, topic);
            }
            return filterTopics(filterIds, topics, queryresult);
        }

        private void extractFilter(ResultSet resultSet, SubTopicIndexDocument topic) throws SQLException {
            URI filterPublicId = getURI(resultSet, "filter_public_id");
            if (null != filterPublicId) {
                TopicFilterIndexDocument filter = new TopicFilterIndexDocument() {{
                    id = filterPublicId;
                    name = resultSet.getString("filter_name");
                    relevanceId = getURI(resultSet, "relevance_public_id");
                }};

                topic.filters.add(filter);
            }
        }

        private List<SubTopicIndexDocument> filterTopics(URI[] filterIds, Map<URI, SubTopicIndexDocument> topics, List<SubTopicIndexDocument> queryresult) {
            if (filterIds != null && filterIds.length > 0) {
                Set<SubTopicIndexDocument> result = new HashSet<>();
                List<URI> filtersInQuery = asList(filterIds);
                for (SubTopicIndexDocument doc : queryresult) {
                    if (filtersInQuery.contains(doc.filterPublicId) || filtersInQuery.contains(doc.resourceFilterId)) {
                        result.add(doc);
                        SubTopicIndexDocument current = doc;
                        while ((current = topics.get(current.parent)) != null) {
                            result.add(current);
                        }
                    }
                }
                return new ArrayList<>(result);
            } else {
                return queryresult;
            }
        }

        private SubTopicIndexDocument extractTopic(URI relevance, ResultSet resultSet, Map<URI, SubTopicIndexDocument> topics, List<SubTopicIndexDocument> queryresult, URI public_id) throws SQLException {
            SubTopicIndexDocument topic = topics.get(public_id);
            if (topic == null) {
                topic = new SubTopicIndexDocument() {{
                    name = resultSet.getString("name");
                    id = public_id;
                    contentUri = getURI(resultSet, "content_uri");
                    parent = getURI(resultSet, "parent_public_id");
                    connectionId = getURI(resultSet, "connection_public_id");
                    topicFilterId = getURI(resultSet, "topic_filter_public_id");
                    filterPublicId = getURI(resultSet, "filter_public_id");
                    resourceFilterId = getURI(resultSet, "resource_filter_public_id");
                    isPrimary = resultSet.getBoolean("is_primary");
                    rank = resultSet.getInt("rank");
                }};
                topics.put(topic.id, topic);
                filterTopicByRelevance(relevance, resultSet, queryresult, topic);
            }
            return topic;
        }

        private void filterTopicByRelevance(URI relevance, ResultSet resultSet, List<SubTopicIndexDocument> queryresult, SubTopicIndexDocument topic) throws SQLException {
            if (relevance == null || relevance.toString().equals("")) {
                queryresult.add(topic);
            } else {
                URI topicRelevance = toURI(resultSet.getString("relevance_public_id"));
                if (topicRelevance != null && topicRelevance.equals(relevance)) {
                    queryresult.add(topic);
                }
            }
        }
    }

    class SubjectQueryExtractor {
        List<SubjectIndexDocument> extractSubjects(ResultSet resultSet) throws SQLException {
            List<SubjectIndexDocument> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new SubjectIndexDocument() {{
                    name = resultSet.getString("subject_name");
                    id = getURI(resultSet, "subject_public_id");
                    contentUri = getURI(resultSet, "subject_content_uri");
                    path = resultSet.getString("subject_path");
                }});
            }
            return result;
        }
    }

    class FilterQueryExtractor {
        List<FilterIndexDocument> extractFilters(ResultSet resultSet) throws SQLException {
            List<FilterIndexDocument> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new FilterIndexDocument() {{
                    name = resultSet.getString("filter_name");
                    id = getURI(resultSet, "filter_public_id");
                }});
            }
            return result;
        }
    }
}
