package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Topic;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "topics")
public class TopicResource {


    private OrientGraphFactory factory;

    public TopicResource(OrientGraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    public List<TopicIndexDocument> index() throws Exception {
        List<TopicIndexDocument> result = new ArrayList<>();

        try (OrientGraph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, name from V_Topic");
            resultSet.iterator().forEachRemaining(record -> {
                TopicIndexDocument document = new TopicIndexDocument();
                result.add(document);
                document.id = URI.create(record.field("id"));
                document.name = record.field("name");
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    public TopicIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic topic = Topic.getById(id, graph);
            TopicIndexDocument result = new TopicIndexDocument(topic);
            transaction.rollback();
            return result;
        }
    }

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody CreateTopicCommand command) throws Exception {
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic topic = new Topic(graph);
            if (null != command.id) topic.setId(command.id.toString());
            topic.name(command.name);

            URI location = URI.create("/topics/" + topic.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        } catch (ORecordDuplicatedException e) {
            throw new DuplicateIdException("" + command.id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic topic = Topic.getById(id, graph);
            topic.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity put(@PathVariable("id") String id, @RequestBody UpdateTopicCommand command) throws Exception {
        try (Graph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            Topic topic = Topic.getById(id, graph);
            topic.setName(command.name);
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    public static class CreateTopicCommand {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;
    }

    public static class UpdateTopicCommand {
        @JsonProperty
        public String name;
    }

    public static class TopicIndexDocument {
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

        public TopicIndexDocument(Map<String, Object> m) {
            id = URI.create((String) ((List) m.get("id")).get(0));
            name = (String) ((List) m.get("name")).get(0);
        }
    }
}
