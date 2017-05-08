package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.repositories.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;
import static no.ndla.taxonomy.service.rest.v1.UrlResolver.getPathMostCloselyMatchingContext;

@RestController
@Transactional
@RequestMapping(path = {"/v1/subjects", "/subjects"})
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
    public List<SubjectIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        List<Object> args = singletonList(language);
        return getSubjectIndexDocuments(GET_SUBJECTS_QUERY, args);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single subject", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public SubjectIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        String sql = GET_SUBJECTS_QUERY.replace("1 = 1", "s.public_id = ?");
        List<Object> args = asList(language, id.toString());

        return getFirst(getSubjectIndexDocuments(sql, args), "Subject", id);
    }

    private List<SubjectIndexDocument> getSubjectIndexDocuments(String sql, List<Object> args) {
        return jdbcTemplate.query(sql, setQueryParameters(args),
                resultSet -> {
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
        );
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a subject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "subject", value = "The updated subject") @RequestBody UpdateSubjectCommand command
    ) throws Exception {
        doPut(id, command);
    }

    @PutMapping
    @ApiOperation(value = "Replaces a collection of subjects")
    public void putSubjects(@ApiParam(name = "subjects", value = "A list of subjects") @RequestBody CreateSubjectCommand[] commands) throws Exception {
        subjectRepository.deleteAll();
        for (CreateSubjectCommand command : commands) {
            post(command);
        }
    }

    @GetMapping("/{id}/topics")
    @ApiOperation(value = "Gets all topics associated with a subject", notes = "This resource is read-only. To update the relationship between subjects and topics, use the resource /subject-topics.")
    public List<TopicIndexDocument> getTopics(
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
                    URI[] filterIds
    ) throws Exception {
        String sql = GET_TOPICS_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY;
        if (!recursive) sql = sql.replace("1 = 1", "t.level = 0");

        List<Object> args = new ArrayList<>();
        args.add(id.toString());
        args.add(language);

        Map<URI, TopicIndexDocument> topics = new HashMap<>();

        return jdbcTemplate.query(sql, setQueryParameters(args),
                resultSet -> {
                    List<TopicIndexDocument> queryresult = new ArrayList<>();
                    String context = "/" + id.toString().substring(4);
                    while (resultSet.next()) {
                        URI public_id = getURI(resultSet, "public_id");

                        TopicIndexDocument topic = topics.get(public_id);
                        if (topic == null) {
                            topic = new TopicIndexDocument() {{
                                name = resultSet.getString("name");
                                id = public_id;
                                contentUri = getURI(resultSet, "content_uri");
                                parent = getURI(resultSet, "parent_public_id");
                                connectionId = getURI(resultSet, "connection_public_id");
                                topicFilterId = getURI(resultSet, "topic_filter_public_id");
                                resourceFilterId = getURI(resultSet, "resource_filter_public_id");
                            }};
                            topics.put(topic.id, topic);
                            queryresult.add(topic);
                        }
                        topic.path = getPathMostCloselyMatchingContext(context, topic.path, resultSet.getString("topic_path"));
                    }

                    if (filterIds.length > 0) {
                        Set<TopicIndexDocument> result = new HashSet<>();
                        for (TopicIndexDocument topic : queryresult) {
                            if (asList(filterIds).contains(topic.resourceFilterId) || asList(filterIds).contains(topic.topicFilterId)) {
                                result.add(topic);
                                TopicIndexDocument current = topic;
                                while (current != null) {
                                    current = topics.get(current.parent);
                                    if (null != current) result.add(current);
                                }
                            }
                        }

                        return new ArrayList<TopicIndexDocument>(result);
                    } else {
                        return queryresult;
                    }
                }
        );
    }

    private String addFilterToQuery(URI[] filterIds, String sql, List<Object> args) {
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

    @PostMapping
    @ApiOperation(value = "Creates a new subject")
    public ResponseEntity<Void> post(@ApiParam(name = "subject", value = "The new subject") @RequestBody CreateSubjectCommand command) throws Exception {
        return doPost(new Subject(), command);
    }

    @GetMapping("/{id}/resources")
    @ApiOperation(value = "Gets all resources for a subject (all topics and subtopics)")
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
                    URI[] filterIds
    ) {
        List<Object> args = new ArrayList<>();
        args.add(subjectId.toString());
        args.add(language);
        args.add(language);

        String sql = GET_RESOURCES_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY;
        if (resourceTypeIds.length > 0) {
            StringBuilder where = new StringBuilder();
            for (URI resourceTypeId : resourceTypeIds) {
                where.append("rt.public_id = ? OR ");
                args.add(resourceTypeId.toString());
            }
            where.setLength(where.length() - " OR ".length());
            sql = sql.replace("1 = 1", "(" + where + ")");
        }

        sql = addFilterToQuery(filterIds, sql, args);

        return jdbcTemplate.query(sql, setQueryParameters(args), resultSet -> {
            List<ResourceIndexDocument> result = new ArrayList<>();
            Map<URI, ResourceIndexDocument> resources = new HashMap<>();


            String context = "/" + subjectId.toString().substring(4);

            while (resultSet.next()) {
                URI id = toURI(resultSet.getString("resource_public_id"));

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
                    result.add(resource);
                }

                resource.path = getPathMostCloselyMatchingContext(context, resource.path, resultSet.getString("resource_path"));

                URI resource_type_id = getURI(resultSet, "resource_type_public_id");
                if (resource_type_id != null) {
                    ResourceTypeIndexDocument resourceType = new ResourceTypeIndexDocument() {{
                        id = resource_type_id;
                        name = resultSet.getString("resource_type_name");
                    }};

                    resource.resourceTypes.add(resourceType);
                }

                URI filterPublicId = getURI(resultSet, "filter_public_id");
                if (null != filterPublicId) {
                    ResourceFilterIndexDocument filter = new ResourceFilterIndexDocument() {{
                        id = filterPublicId;
                        relevanceId = getURI(resultSet, "relevance_public_id");
                    }};

                    resource.filters.add(filter);
                }
            }
            return result;
        });
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters for a subject")
    public List<FilterIndexDocument> getFilters(@PathVariable("id") URI subjectId) {
        String sql = GET_FILTERS_BY_SUBJECT_PUBLIC_ID_QUERY;
        List<Object> args = singletonList(subjectId.toString());

        return jdbcTemplate.query(sql, setQueryParameters(args),
                resultSet -> {
                    List<FilterIndexDocument> result = new ArrayList<>();
                    while (resultSet.next()) {
                        result.add(new FilterIndexDocument() {{
                            name = resultSet.getString("filter_name");
                            id = getURI(resultSet, "filter_public_id");
                        }});
                    }
                    return result;
                }
        );
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

    public static class TopicIndexDocument {
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
        @ApiModelProperty(value = "The path part of the url to this topic.", example = "/subject:1/topic:1")
        public String path;

        @JsonProperty
        @ApiModelProperty(value = "The id of the subject-topics or topic-subtopics connection which causes this topic to be included in the result set.", example = "urn:subject-topic:1")
        public URI connectionId;

        @JsonIgnore
        URI topicFilterId, resourceFilterId;

        @Override
        @JsonIgnore
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TopicIndexDocument)) return false;

            TopicIndexDocument that = (TopicIndexDocument) o;

            return id.equals(that.id);
        }

        @Override
        @JsonIgnore
        public int hashCode() {
            return id.hashCode();
        }
    }

    public static class ResourceIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:12")
        public URI topicId;

        @JsonProperty
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "Resource type(s)", example = "[{id = 'urn:resource-type:1', name = 'lecture'}]")
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

    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Resource type id", example = "urn:resource-type:12")
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

    public static class FilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Filter id", example = "urn:filter:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Filter name", example = "1T-YF")
        public String name;
    }

    public static class ResourceFilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Filter id", example = "urn:filter:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "ID of the relevance the resource has in context of the filter", example = "urn:relevance:core")
        public URI relevanceId;
    }
}
