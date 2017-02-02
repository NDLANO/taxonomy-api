package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.repositories.SubjectRepository;
import no.ndla.taxonomy.service.repositories.TopicRepository;
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

import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;

@RestController
@RequestMapping(path = {"subjects", "/v1/subjects"})
@Transactional
public class Subjects {
    private SubjectRepository subjectRepository;
    private TopicRepository topicRepository;

    private static final String queryBase = getQuery("get_resources_by_subject_public_id_recursively");
    private JdbcTemplate jdbcTemplate;

    public Subjects(SubjectRepository subjectRepository, TopicRepository topicRepository, JdbcTemplate jdbcTemplate) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @ApiOperation("Gets all subjects")
    public List<SubjectIndexDocument> index() throws Exception {
        List<SubjectIndexDocument> result = new ArrayList<>();
        Iterable<Subject> all = subjectRepository.findAll();
        all.forEach(subject -> result.add(new SubjectIndexDocument(subject)));
        return result;
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single subject")
    public SubjectIndexDocument get(@PathVariable("id") URI id) throws Exception {
        Subject subject = subjectRepository.getByPublicId(id);
        SubjectIndexDocument result = new SubjectIndexDocument(subject);
        return result;
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
            @PathVariable("id") URI id,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
            @ApiParam("If true, subtopics are fetched recursively")
                    boolean recursive
    ) throws Exception {
        List<TopicIndexDocument> results = new ArrayList<>();
        List<Topic> topics = topicRepository.getBySubjectTopicsSubjectPublicId(id);
        topics.iterator().forEachRemaining(t -> results.add(new TopicIndexDocument(t, recursive)));
        return results;
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
            throw new DuplicateIdException(command.id.toString());
        }
    }

    @GetMapping("/{id}/resources")
    @ApiOperation(value = "Gets all resources for a subject (all topics and subtopics)")
    public List<ResourceIndexDocument> getResources(
            @PathVariable("id") URI subjectId,
            @RequestParam(value = "type", required = false, defaultValue = "")
            @ApiParam(value = "Filter by resource type id(s). If not specified, resources of all types will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] resourceTypeIds
    ) {
        List<Object> args = new ArrayList<>();
        args.add(subjectId.toString());
        String sql;
        if (resourceTypeIds.length > 0) {
            StringBuilder where = new StringBuilder();
            for (URI resourceTypeId : resourceTypeIds) {
                where.append("rt.public_id = ? OR ");
                args.add(resourceTypeId.toString());
            }
            where.setLength(where.length() - 4);
            sql = queryBase.replace("1 = 1", "(" + where + ")");
        } else {
            sql = queryBase;
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

        SubjectIndexDocument() {
        }

        SubjectIndexDocument(Subject subject) {
            id = subject.getPublicId();
            name = subject.getName();
            contentUri = subject.getContentUri();
        }
    }

    public static class TopicIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        @JsonProperty
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @ApiModelProperty("Children of this topic")
        public TopicIndexDocument[] subtopics;

        @JsonProperty
        @ApiModelProperty(notes = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        TopicIndexDocument() {
        }

        TopicIndexDocument(Topic topic, boolean recursive) {
            id = topic.getPublicId();
            name = topic.getName();
            contentUri = topic.getContentUri();
            if (recursive) addSubtopics(topic);
        }

        private void addSubtopics(Topic topic) {
            ArrayList<TopicIndexDocument> result = new ArrayList<>();
            Iterator<Topic> subtopics = topic.getSubtopics();
            while (subtopics.hasNext()) {
                result.add(new TopicIndexDocument(subtopics.next(), true));
            }
            this.subtopics = result.toArray(new TopicIndexDocument[result.size()]);
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
        public List<ResourceTypeIndexDocument> resourceTypes = new ArrayList<>();

        @JsonProperty
        @ApiModelProperty(notes = "The ID of this resource in the system where the content is stored. " +
                "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
        public URI contentUri;

        ResourceIndexDocument() {
        }

        ResourceIndexDocument(Resource resource) {
            this.id = resource.getPublicId();
            this.name = resource.getName();
        }
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
