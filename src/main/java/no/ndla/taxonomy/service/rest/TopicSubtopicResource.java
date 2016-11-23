package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicSubtopic;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping(path = "topic-subtopics")
public class TopicSubtopicResource {

    private TitanGraph graph;

    public TopicSubtopicResource(TitanGraph graph) {
        this.graph = graph;
    }

    @GetMapping
    public List<TopicSubtopicIndexDocument> index() {
        List<TopicSubtopicIndexDocument> result = new ArrayList<>();

        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            GraphTraversal<Edge, Edge> topics = transaction.traversal().E().hasLabel(TopicSubtopic.LABEL);
            topics.forEachRemaining(v -> result.add(new TopicSubtopicIndexDocument(new TopicSubtopic(v))));
        }

        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") String id) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            TopicSubtopic subjectTopic = TopicSubtopic.getById(id, transaction);
            return ResponseEntity.ok(new TopicSubtopicIndexDocument(subjectTopic));
        }
    }

    @PostMapping
    public ResponseEntity post(@RequestBody AddSubtopicToTopicCommand command) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {

            Topic topic = Topic.getById(command.topicid.toString(), transaction);
            Topic subtopic = Topic.getById(command.subtopicid.toString(), transaction);

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
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            TopicSubtopic subjectTopic = TopicSubtopic.getById(id, transaction);
            subjectTopic.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> put(@PathVariable("id") String id, @RequestBody UpdateTopicSubtopicCommand command) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            TopicSubtopic subjectTopic = TopicSubtopic.getById(id, transaction);
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
