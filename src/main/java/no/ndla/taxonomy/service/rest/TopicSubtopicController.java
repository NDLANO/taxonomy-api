package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
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
@RequestMapping(path = "topic-subtopics")
public class TopicSubtopicController {
    private GraphFactory factory;

    public TopicSubtopicController(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    public List<TopicSubtopicIndexDocument> index() throws Exception {
        List<TopicSubtopicIndexDocument> result = new ArrayList<>();

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            GraphTraversal<Edge, Edge> topics = graph.traversal().E().hasLabel(TopicSubtopic.LABEL);
            topics.forEachRemaining(v -> result.add(new TopicSubtopicIndexDocument(new TopicSubtopic(v))));
            transaction.rollback();
        }

        return result;
    }

    @GetMapping("/{id}")
    public TopicSubtopicIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicSubtopic subjectTopic = TopicSubtopic.getById(id, graph);
            TopicSubtopicIndexDocument result = new TopicSubtopicIndexDocument(subjectTopic);
            transaction.rollback();
            return result;
        }
    }

    @PostMapping
    public ResponseEntity post(@RequestBody AddSubtopicToTopicCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {

            Topic topic = Topic.getById(command.topicid.toString(), graph);
            Topic subtopic = Topic.getById(command.subtopicid.toString(), graph);

            Iterator<Topic> topics = topic.getSubtopics();
            while (topics.hasNext()) {
                Topic t = topics.next();
                if (t.getId().equals(subtopic.getId()))
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("Subject with id " + command.topicid + " already contains topic with id " + command.subtopicid);
            }

            TopicSubtopic subjectTopic = topic.addSubtopic(subtopic);
            subjectTopic.setPrimary(command.primary);

            URI location = URI.create("/subject-topics/" + subjectTopic.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicSubtopic subjectTopic = TopicSubtopic.getById(id, graph);
            subjectTopic.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> put(@PathVariable("id") String id, @RequestBody UpdateTopicSubtopicCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicSubtopic subjectTopic = TopicSubtopic.getById(id, graph);
            subjectTopic.setPrimary(command.primary);
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    public static class AddSubtopicToTopicCommand {
        @JsonProperty
        public URI topicid, subtopicid;

        @JsonProperty
        public boolean primary;
    }

    public static class UpdateTopicSubtopicCommand {
        @JsonProperty
        public URI id;

        @JsonProperty
        public boolean primary;
    }

    public static class TopicSubtopicIndexDocument {
        @JsonProperty
        public URI topicid, subtopicid, id;

        @JsonProperty
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
