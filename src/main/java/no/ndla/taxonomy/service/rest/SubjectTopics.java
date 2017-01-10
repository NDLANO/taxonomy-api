package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.SubjectTopic;
import no.ndla.taxonomy.service.domain.Topic;
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
@RequestMapping(path = "subject-topics")
public class SubjectTopics {

    private GraphFactory factory;

    public SubjectTopics(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    public List<SubjectTopicIndexDocument> index() throws Exception {
        List<SubjectTopicIndexDocument> result = new ArrayList<>();

        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, primary, in.id as topicid, out.id as subjectid from `E_subject-has-topics`");
            resultSet.iterator().forEachRemaining(record -> {
                SubjectTopicIndexDocument document = new SubjectTopicIndexDocument();
                result.add(document);
                document.id = URI.create(record.field("id"));
                document.subjectid = URI.create(record.field("subjectid"));
                document.topicid = URI.create(record.field("topicid"));
                document.primary = Boolean.valueOf(record.field("primary"));
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    public SubjectTopicIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            SubjectTopic subjectTopic = SubjectTopic.getById(id, graph);
            SubjectTopicIndexDocument result = new SubjectTopicIndexDocument(subjectTopic);
            transaction.rollback();
            return result;
        }
    }

    @PostMapping
    public ResponseEntity post(@RequestBody AddTopicToSubjectCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {

            Subject subject = null; //subjectRepository.getById(command.subjectid);
            Topic topic = Topic.getById(command.topicid.toString(), graph);

            Iterator<Topic> topics = subject.getTopics();
            while (topics.hasNext()) {
                Topic t = topics.next();
                if (t.getId().equals(topic.getId()))
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("Subject with id " + command.subjectid + " already contains topic with id " + command.topicid);
            }

            SubjectTopic subjectTopic = subject.addTopic(topic);
            subjectTopic.setPrimary(command.primary);

            URI location = URI.create("/subject-topics/" + subjectTopic.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            SubjectTopic subjectTopic = SubjectTopic.getById(id, graph);
            subjectTopic.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> put(@PathVariable("id") String id, @RequestBody UpdateSubjectTopicCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            SubjectTopic subjectTopic = SubjectTopic.getById(id, graph);
            subjectTopic.setPrimary(command.primary);
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    public static class AddTopicToSubjectCommand {
        @JsonProperty
        public URI subjectid, topicid;

        @JsonProperty
        public boolean primary;
    }

    public static class UpdateSubjectTopicCommand {
        @JsonProperty
        public URI id;

        @JsonProperty
        public boolean primary;
    }

    public static class SubjectTopicIndexDocument {
        @JsonProperty
        public URI subjectid, topicid, id;

        @JsonProperty
        public boolean primary;

        SubjectTopicIndexDocument() {
        }

        SubjectTopicIndexDocument(SubjectTopic subjectTopic) {
            id = subjectTopic.getId();
            subjectid = subjectTopic.getSubject().getId();
            topicid = subjectTopic.getTopic().getId();
            primary = subjectTopic.isPrimary();
        }
    }
}
