package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.SubjectTopic;
import no.ndla.taxonomy.service.domain.Topic;
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
@RequestMapping(path = "subject-topics")
public class SubjectTopicResource {

    private TitanGraph graph;

    public SubjectTopicResource(TitanGraph graph) {
        this.graph = graph;
    }

    @GetMapping
    public List<SubjectTopicIndexDocument> index() {
        List<SubjectTopicIndexDocument> result = new ArrayList<>();

        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            GraphTraversal<Edge, Edge> topics = transaction.traversal().E().hasLabel(SubjectTopic.LABEL);
            topics.forEachRemaining(v -> result.add(new SubjectTopicIndexDocument(new SubjectTopic(v))));
        }

        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") String id) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {
            SubjectTopic subjectTopic = SubjectTopic.getById(id, transaction);
            return ResponseEntity.ok(new SubjectTopicIndexDocument(subjectTopic));
        }
    }

    @PostMapping
    public ResponseEntity post(@RequestBody AddTopicToSubjectCommand command) {
        try (TitanTransaction transaction = graph.buildTransaction().start()) {

            Subject subject = Subject.getById(command.subjectid, transaction);
            Topic topic = Topic.getById(command.topicid, transaction);

            Iterator<Topic> topics = subject.getTopics();
            while (topics.hasNext()) {
                Topic t = topics.next();
                if (t.getId().equals(topic.getId()))
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("Subject with id " + command.subjectid + " already contains topic with id " + command.topicid);
            }

            SubjectTopic subjectTopic = subject.addTopic(topic);
            subjectTopic.setPrimary(command.primary);

            transaction.commit();
            return ResponseEntity.created(URI.create("/subject-topics/" + subjectTopic.getId())).build();
        }
    }


    public static class AddTopicToSubjectCommand {
        @JsonProperty
        public Object subjectid, topicid;
        @JsonProperty
        public boolean primary;
    }

    private class SubjectTopicIndexDocument {
        @JsonProperty
        public Object subjectid, topicid;

        @JsonProperty
        public boolean primary;

        SubjectTopicIndexDocument(SubjectTopic subjectTopic) {
            subjectid = subjectTopic.getSubject().getId();
            topicid = subjectTopic.getTopic().getId();
            primary = subjectTopic.isPrimary();
        }
    }
}
