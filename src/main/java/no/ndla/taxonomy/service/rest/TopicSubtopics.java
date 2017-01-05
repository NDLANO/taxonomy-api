package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import no.ndla.taxonomy.service.GraphFactory;
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
@RequestMapping(path = "topic-subtopics")
public class TopicSubtopics {
    private GraphFactory factory;

    public TopicSubtopics(GraphFactory factory) {
        this.factory = factory;
    }


    @GetMapping
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
                document.primary = Boolean.valueOf(record.field("primary"));
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    public TopicSubtopicIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicSubtopic topicSubtopic = TopicSubtopic.getById(id, graph);
            TopicSubtopicIndexDocument result = new TopicSubtopicIndexDocument(topicSubtopic);
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
                            .body("Topic with id " + command.topicid + " already contains topic with id " + command.subtopicid);
            }

            TopicSubtopic topicSubtopic = topic.addSubtopic(subtopic);
            topicSubtopic.setPrimary(command.primary);

            URI location = URI.create("/topic-subtopics/" + topicSubtopic.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicSubtopic topicSubtopic = TopicSubtopic.getById(id, graph);
            topicSubtopic.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> put(@PathVariable("id") String id, @RequestBody UpdateTopicSubtopicCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicSubtopic topicSubtopic = TopicSubtopic.getById(id, graph);
            topicSubtopic.setPrimary(command.primary);
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
