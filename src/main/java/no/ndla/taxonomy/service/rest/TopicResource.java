package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.Topic;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.T;
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
            GraphTraversal<Vertex, Vertex> Topics = transaction.traversal().V().hasLabel("topic");
            Topics.forEachRemaining(v -> result.add(new TopicIndexDocument(new Topic(v))));
        }

        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") String id) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            GraphTraversal<Vertex, Vertex> traversal = transaction.traversal().V(id);
            if (traversal.hasNext()) return ResponseEntity.ok(new TopicIndexDocument(new Topic(traversal.next())));
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody CreateTopicCommand command) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            Vertex vertex = transaction.addVertex(T.label, "topic");
            Topic topic = new Topic(vertex);
            topic.name(command.name);

            transaction.commit();
            return ResponseEntity.created(URI.create("/topics/" + vertex.id())).build();
        }
    }


    public static class CreateTopicCommand {
        @JsonProperty
        private String name;
    }

    private class TopicIndexDocument {
        @JsonProperty
        public Object id;

        @JsonProperty
        public String name;

        TopicIndexDocument(Topic topic) {
            id = topic.getId();
            name = topic.getName();
        }
    }
}
