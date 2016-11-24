package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.Topic;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
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
            Subject subject = Subject.getById(id, transaction);
            return ResponseEntity.ok(new SubjectIndexDocument(subject));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") String id) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            Subject subject = Subject.getById(id, transaction);
            subject.remove();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity put(@PathVariable("id") String id, @RequestBody UpdateSubjectCommand command) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            Subject subject = Subject.getById(id, transaction);
            subject.setName(command.name);
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/{id}/topics")
    public ResponseEntity getTopics(
            @PathVariable("id") String id,
            @RequestParam(value = "recursive", required = false, defaultValue = "false") boolean recursive) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            List<TopicIndexDocument> results = new ArrayList<>();
            Subject subject = Subject.getById(id, transaction);
            subject.getTopics().forEachRemaining(t -> results.add(new TopicIndexDocument(t, recursive)));
            return ResponseEntity.ok(results);
        }
    }

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody CreateSubjectCommand command) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            Subject subject = new Subject(transaction);
            subject.name(command.name);

            URI location = URI.create("/subjects/" + subject.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    static class CreateSubjectCommand {
        @JsonProperty
        public String name;
    }

    static class UpdateSubjectCommand {
        @JsonProperty
        public String name;
    }

    static class SubjectIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        SubjectIndexDocument() {
        }

        SubjectIndexDocument(Subject subject) {
            id = subject.getId();
            name = subject.getName();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class TopicIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        @JsonProperty
        public TopicIndexDocument[] subtopics;

        TopicIndexDocument() {
        }

        TopicIndexDocument(Topic topic, boolean recursive) {
            id = topic.getId();
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
