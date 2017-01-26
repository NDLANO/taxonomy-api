package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;
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
@RequestMapping(path = {"topic-subtopics", "/v1/topic-subtopics"})
public class TopicSubtopics {
    private GraphFactory factory;

    public TopicSubtopics(GraphFactory factory) {
        this.factory = factory;
    }


    @GetMapping
    @ApiOperation(value="Gets all connections between topics and subtopics")
    public List<TopicSubtopicIndexDocument> index() throws Exception {
        List<TopicSubtopicIndexDocument> result = new ArrayList<>();

        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, primary, out.id as topicid, in.id as subtopicid from `E_topic-has-subtopics`");
            resultSet.iterator().forEachRemaining(record -> {
                TopicSubtopicIndexDocument document = new TopicSubtopicIndexDocument();
                result.add(document);
                document.id = URI.create(record.field("id"));
                document.topicid = URI.create(record.field("topicid"));
                document.subtopicid = URI.create(record.field("subtopicid"));
                Boolean primary = record.field("primary", OType.BOOLEAN);
                document.primary = primary == null ? false : primary;
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single connection between a topic and a subtopic")
    public TopicSubtopicIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicSubtopic topicSubtopic = TopicSubtopic.getById(id, graph);
            TopicSubtopicIndexDocument result = new TopicSubtopicIndexDocument(topicSubtopic);
            transaction.rollback();
            return result;
        }
    }

    @PostMapping
    @ApiOperation(value = "Adds a subtopic to a topic")
    public ResponseEntity<Void> post(
            @ApiParam(name="connection", value = "The new connection") @RequestBody AddSubtopicToTopicCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {

            Topic topic = Topic.getById(command.topicid.toString(), graph);
            Topic subtopic = Topic.getById(command.subtopicid.toString(), graph);

            Iterator<Topic> topics = topic.getSubtopics();
            while (topics.hasNext()) {
                Topic t = topics.next();
                if (t.getId().equals(subtopic.getId())) {
                    throw new DuplicateIdException("Topic with id " + command.topicid + " already contains topic with id " + command.subtopicid);
                }
            }

            TopicSubtopic topicSubtopic = topic.addSubtopic(subtopic);
            topicSubtopic.setPrimary(command.primary);

            URI location = URI.create("/topic-subtopics/" + topicSubtopic.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Removes a connection between a topic and a subtopic")
    public void delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicSubtopic topicSubtopic = TopicSubtopic.getById(id, graph);
            topicSubtopic.remove();
            transaction.commit();
        }
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Updates a connection between a topic and a subtopic", notes = "Use to update which topic is primary to a subtopic")
    public void put(@PathVariable("id") String id,
                    @ApiParam(name = "connection", value = "The updated connection") @RequestBody UpdateTopicSubtopicCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicSubtopic topicSubtopic = TopicSubtopic.getById(id, graph);
            topicSubtopic.setPrimary(command.primary);
            transaction.commit();
        }
    }

    public static class AddSubtopicToTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Subtopic id", example = "urn:topic:234")
        public URI subtopicid;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example="true")
        public boolean primary;
    }

    public static class UpdateTopicSubtopicCommand {
        @JsonProperty
        @ApiModelProperty(value = "Connection id", example="urn:topic-has-subtopics:345")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example="true")
        public boolean primary;
    }

    public static class TopicSubtopicIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Subtopic id", example = "urn:topic:234")
        public URI subtopicid;

        @JsonProperty
        @ApiModelProperty(value = "Connection id", example="urn:topic-has-subtopics:345")
        public URI  id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example="true")
        public boolean primary;

        TopicSubtopicIndexDocument() {
        }

        TopicSubtopicIndexDocument(TopicSubtopic topicSubtopic) {
            id = topicSubtopic.getId();
            topicid = topicSubtopic.getTopic().getId();
            subtopicid = topicSubtopic.getSubtopic().getId();
            primary = topicSubtopic.isPrimary();
        }
    }
}
