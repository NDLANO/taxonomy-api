package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.SubjectTopic;
import no.ndla.taxonomy.service.domain.Topic;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping(path = {"subject-topics", "/v1/subject-topics"})
public class SubjectTopics {

    private GraphFactory factory;

    public SubjectTopics(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    @ApiOperation("Gets all connections between a subject and a topic")
    public List<SubjectTopicIndexDocument> index() throws Exception {
        List<SubjectTopicIndexDocument> result = new ArrayList<>();

        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, primary, in.id as topicid, out.id as subjectid from `E_subject-has-topics`");
            resultSet.iterator().forEachRemaining(record -> {
                SubjectTopicIndexDocument document = new SubjectTopicIndexDocument();
                result.add(document);
                document.id = URI.create(record.field("id"));
                document.subjectid = URI.create(record.field("subjectid"));
                document.topicid = URI.create(record.field("topicid"));
                document.primary = Boolean.valueOf(record.field("primary"));
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("Get a specific connection between a subject and a topic")
    public SubjectTopicIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            SubjectTopic subjectTopic = SubjectTopic.getById(id, graph);
            SubjectTopicIndexDocument result = new SubjectTopicIndexDocument(subjectTopic);
            transaction.rollback();
            return result;
        }
    }

    @PostMapping
    @ApiOperation("Add a new topic to a subject")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> post(
            @ApiParam(name = "command", value = "The subject and topic getting connected. Use primary=true if primary connection for this topic.") @RequestBody AddTopicToSubjectCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {

            Subject subject = null; //subjectRepository.getById(command.subjectid);
            Topic topic = Topic.getById(command.topicid.toString(), graph);

            Iterator<Topic> topics = subject.getTopics();
            while (topics.hasNext()) {
                Topic t = topics.next();
                if (t.getId().equals(topic.getId()))
                    throw new DuplicateIdException("Subject with id " + command.subjectid + " already contains topic with id " + command.topicid);
            }

            SubjectTopic subjectTopic = subject.addTopic(topic);
            subjectTopic.setPrimary(command.primary);

            URI location = URI.create("/subject-topics/" + subjectTopic.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Deletes a connection between a subject and a topic")
    public void delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            SubjectTopic subjectTopic = SubjectTopic.getById(id, graph);
            subjectTopic.remove();
            transaction.commit();
        }
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Updates a connection between subject and topic. Use to update which subject is primary to a topic.")
    public void put(@PathVariable("id") String id, @RequestBody UpdateSubjectTopicCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            SubjectTopic subjectTopic = SubjectTopic.getById(id, graph);
            subjectTopic.setPrimary(command.primary);
            transaction.commit();
        }
    }

    public static class AddTopicToSubjectCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Subject id", example = "urn:subject:123")
        public URI subjectid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example="true")
        public boolean primary;
    }

    public static class UpdateSubjectTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "connection id", example = "urn:subject-has-topics:2")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "primary", example = "true")
        public boolean primary;
    }

    public static class SubjectTopicIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Subject id", example="urn:subject:123")
        public URI subjectid;

        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Connection id", example = "urn:subject-has-topics:34")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "primary", example = "true")
        public boolean primary;

        SubjectTopicIndexDocument() {
        }

        SubjectTopicIndexDocument(SubjectTopic subjectTopic) {
            id = subjectTopic.getId();
            subjectid = subjectTopic.getSubject().getId();
            topicid = subjectTopic.getTopic().getId();
            primary = subjectTopic.isPrimary();
        }
    }
}
