package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.Topic;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping(path = "subjects")
public class SubjectController {

    private GraphFactory factory;

    public SubjectController(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    public List<SubjectIndexDocument> index() throws Exception {
        List<SubjectIndexDocument> result = new ArrayList<>();

        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, name from V_Subject");
            resultSet.iterator().forEachRemaining(record -> {
                SubjectIndexDocument document = new SubjectIndexDocument();
                result.add(document);
                document.id = URI.create(record.field("id"));
                document.name = record.field("name");
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    public SubjectIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject subject = Subject.getById(id, graph);
            SubjectIndexDocument result = new SubjectIndexDocument(subject);
            transaction.rollback();
            return result;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject subject = Subject.getById(id, graph);
            subject.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity put(@PathVariable("id") String id, @RequestBody UpdateSubjectCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject subject = Subject.getById(id, graph);
            subject.setName(command.name);
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/{id}/topics")
    public List<TopicIndexDocument> getTopics(
            @PathVariable("id") String id,
            @RequestParam(value = "recursive", required = false, defaultValue = "false") boolean recursive) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            List<TopicIndexDocument> results = new ArrayList<>();
            Subject subject = Subject.getById(id, graph);
            subject.getTopics().forEachRemaining(t -> results.add(new TopicIndexDocument(t, recursive)));
            transaction.rollback();
            return results;
        }
    }

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody CreateSubjectCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Subject subject = new Subject(graph);
            if (null != command.id) subject.setId(command.id.toString());
            subject.name(command.name);
            URI location = URI.create("/subjects/" + subject.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        } catch (ORecordDuplicatedException e) {
            throw new DuplicateIdException("" + command.id);
        }
    }

    public static class CreateSubjectCommand {
        @JsonProperty
        public URI id;

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
