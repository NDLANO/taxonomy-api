package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Topic;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "topics")
public class TopicResource {

    private TitanGraph graph;

    public TopicResource(TitanGraph graph) {
        this.graph = graph;
    }

    @GetMapping
    public List<TopicIndexDocument> index() {
        List<TopicIndexDocument> result = new ArrayList<>();

        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            GraphTraversal<Vertex, Vertex> topics = transaction.traversal().V().hasLabel(Topic.LABEL);
            topics.forEachRemaining(v -> result.add(new TopicIndexDocument(new Topic(v))));
        }

        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") String id) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            Topic topic = Topic.getById(id, transaction);
            return ResponseEntity.ok(new TopicIndexDocument(topic));
        }
    }

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody CreateTopicCommand command) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            if (null != command.id) validateIdIsUnique(command.id, transaction);

            Topic topic = new Topic(transaction);
            if (null != command.id) topic.setId(command.id.toString());
            topic.name(command.name);

            URI location = URI.create("/topics/" + topic.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    private void validateIdIsUnique(URI id, TitanTransaction transaction) {
        if (null != Topic.findById(id.toString(), transaction))
            throw new DuplicateIdException(id.toString());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") String id) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            Topic topic = Topic.getById(id, transaction);
            topic.remove();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity put(@PathVariable("id") String id, @RequestBody UpdateTopicCommand command) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            Topic topic = Topic.getById(id, transaction);
            topic.setName(command.name);
            return ResponseEntity.noContent().build();
        }
    }

    static class CreateTopicCommand {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;
    }

    static class UpdateTopicCommand {
        @JsonProperty
        public String name;
    }

    static class TopicIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        TopicIndexDocument() {
        }

        TopicIndexDocument(Topic topic) {
            id = topic.getId();
            name = topic.getName();
        }
    }
}
