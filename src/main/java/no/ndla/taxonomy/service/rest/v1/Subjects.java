package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.repositories.SubjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"subjects", "/v1/subjects"})
@Transactional
public class Subjects {
    private static final String GET_SUBJECTS_QUERY = getQuery("get_subjects");
    private static final String GET_RESOURCES_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_resources_by_subject_public_id_recursively");
    private static final String GET_TOPICS_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_topics_by_subject_public_id_recursively");

    private SubjectRepository subjectRepository;
    private JdbcTemplate jdbcTemplate;

    public Subjects(SubjectRepository subjectRepository, JdbcTemplate jdbcTemplate) {
        this.subjectRepository = subjectRepository;
        this.jdbcTemplate = jdbcTemplate;
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
                        }});
                    }
                    return result;
                }
        );
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Deletes a single subject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) throws Exception {
        subjectRepository.getByPublicId(id);
        subjectRepository.deleteByPublicId(id);
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a subject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "subject", value = "The updated subject") @RequestBody UpdateSubjectCommand command
    ) throws Exception {
        Subject subject = subjectRepository.getByPublicId(id);
        subject.setName(command.name);
        subject.setContentUri(command.contentUri);
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
                    boolean recursive
    ) throws Exception {
        String sql = GET_TOPICS_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY;
        if (!recursive) sql = sql.replace("1 = 1", "t.level = 0");
        List<Object> args = asList(id.toString(), language);

        Map<URI, TopicIndexDocument> topics = new HashMap<>();

        return jdbcTemplate.query(sql, setQueryParameters(args),
                resultSet -> {
                    List<TopicIndexDocument> result = new ArrayList<>();
                    while (resultSet.next()) {

                        TopicIndexDocument topic = new TopicIndexDocument() {{
                            name = resultSet.getString("name");
                            id = getURI(resultSet, "public_id");
                            contentUri = getURI(resultSet, "content_uri");
                            parent = getURI(resultSet, "parent_public_id");
                        }};

                        topics.put(topic.id, topic);
                        result.add(topic);
                    }
                    return result;
                }
        );
    }

    @PostMapping
    @ApiOperation(value = "Creates a new subject")
    public ResponseEntity<Void> post(@ApiParam(name = "subject", value = "The new subject") @RequestBody CreateSubjectCommand command) throws Exception {
        try {
            Subject subject = new Subject();
            if (null != command.id) subject.setPublicId(command.id);
            subject.setName(command.name);
            subject.setContentUri(command.contentUri);
            URI location = URI.create("/subjects/" + subject.getPublicId());
            subjectRepository.save(subject);
            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException("" + command.id);
        }
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
                    URI[] resourceTypeIds
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

        return jdbcTemplate.query(sql, setQueryParameters(args), resultSet -> {
            List<ResourceIndexDocument> result = new ArrayList<>();
            ResourceIndexDocument current, previous = null;

            while (resultSet.next()) {
                URI id = toURI(resultSet.getString("resource_public_id"));

                boolean duplicate = previous != null && id.equals(previous.id);
                if (duplicate) {
                    current = previous;
                } else {
                    current = new ResourceIndexDocument() {{
                        topicId = toURI(resultSet.getString("topic_public_id"));
                        contentUri = toURI(resultSet.getString("resource_content_uri"));
                        name = resultSet.getString("resource_name");
                        id = toURI(resultSet.getString("resource_public_id"));
                    }};
                    result.add(current);
                }

                String resource_type_id = resultSet.getString("resource_type_public_id");
                if (resource_type_id != null) {
                    ResourceTypeIndexDocument resourceType = new ResourceTypeIndexDocument() {{
                        id = toURI(resource_type_id);
                        name = resultSet.getString("resource_type_name");
                    }};

                    current.resourceTypes.add(resourceType);
                }
                previous = current;
            }
            return result;
        });
    }

    public static class CreateSubjectCommand {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:subject:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(notes = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the subject", example = "Mathematics")
        public String name;
    }

    public static class UpdateSubjectCommand {
        @JsonProperty
        @ApiModelProperty(notes = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the subject", example = "Mathematics")
        public String name;
    }

    public static class SubjectIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:subject:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(notes = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "The name of the subject", example = "Mathematics")
        public String name;
    }

    public static class TopicIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        @JsonProperty
        @ApiModelProperty(notes = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty("Parent id in the current context, null if none exists")
        public URI parent;
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
        public List<ResourceTypeIndexDocument> resourceTypes = new ArrayList<>();

        @JsonProperty
        @ApiModelProperty(notes = "The ID of this resource in the system where the content is stored. " +
                "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
        public URI contentUri;
    }

    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Resource type id", example = "urn:resource-type:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Resource type name", example = "Assignment")
        public String name;
    }
}
