package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.Subject;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "subjects")
public class SubjectResource {

    private TitanGraph graph;

    public SubjectResource(TitanGraph graph) {
        this.graph = graph;
    }

    @GetMapping
    public List<SubjectIndexDocument> index() {
        List<SubjectIndexDocument> result = new ArrayList<>();

        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            GraphTraversal<Vertex, Vertex> subjects = transaction.traversal().V().hasLabel("subject");
            subjects.forEachRemaining(v -> result.add(new SubjectIndexDocument(new Subject(v))));
        }
        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") String id) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            GraphTraversal<Vertex, Vertex> traversal = transaction.traversal().V(id);
            if (traversal.hasNext()) return ResponseEntity.ok(new SubjectIndexDocument(new Subject(traversal.next())));
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody CreateSubjectCommand command) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            Vertex vertex = transaction.addVertex(T.label, "subject");
            Subject subject = new Subject(vertex);
            subject.name(command.name);
            transaction.commit();
            return ResponseEntity.created(URI.create("/subjects/" + vertex.id())).build();
        }
    }

    public static class CreateSubjectCommand {
        @JsonProperty
        public String name;
    }

    private class SubjectIndexDocument {
        @JsonProperty
        public Object id;

        @JsonProperty
        public String name;

        SubjectIndexDocument(Subject subject) {
            id = subject.getId();
            name = subject.getName();
        }
    }
}
