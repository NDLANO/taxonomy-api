package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.repositories.SubjectRepository;
import no.ndla.taxonomy.service.repositories.TopicRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping(path = {"subjects", "/v1/subjects"})
@Transactional
public class Subjects {
    private SubjectRepository subjectRepository;
    private TopicRepository topicRepository;

    public Subjects(SubjectRepository subjectRepository, TopicRepository topicRepository) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
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
            URI location = URI.create("/subjects/" + subject.getPublicId());
            subjectRepository.save(subject);
            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException(command.id.toString());
        }
    }

    public static class CreateSubjectCommand {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:subject:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the subject", example = "Mathematics")
        public String name;
    }

    public static class UpdateSubjectCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the subject", example = "Mathematics")
        public String name;
    }

    public static class SubjectIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:subject:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the subject", example = "Mathematics")
        public String name;

        SubjectIndexDocument() {
        }

        SubjectIndexDocument(Subject subject) {
            id = subject.getPublicId();
            name = subject.getName();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TopicIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        @JsonProperty
        @ApiModelProperty("Children of this topic")
        public TopicIndexDocument[] subtopics;

        TopicIndexDocument() {
        }

        TopicIndexDocument(Topic topic, boolean recursive) {
            id = topic.getPublicId();
            name = topic.getName();
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
}
