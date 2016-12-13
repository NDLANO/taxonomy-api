package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicResource;
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
@RequestMapping(path = "topic-resources")
public class TopicResourceController {
    private GraphFactory factory;

    public TopicResourceController(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    public List<TopicResourceIndexDocument> index() throws Exception {
        List<TopicResourceIndexDocument> result = new ArrayList<>();

        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, primary, out.id as topicid, in.id as resourceid from `E_topic-has-resources`");
            resultSet.iterator().forEachRemaining(record -> {
                TopicResourceIndexDocument document = new TopicResourceIndexDocument();
                result.add(document);
                document.id = URI.create(record.field("id"));
                document.topicid = URI.create(record.field("topicid"));
                document.resourceid = URI.create(record.field("resourceid"));
                document.primary = Boolean.valueOf(record.field("primary"));
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    public TopicResourceIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicResource topicResource = TopicResource.getById(id, graph);
            TopicResourceIndexDocument result = new TopicResourceIndexDocument(topicResource);
            transaction.rollback();
            return result;
        }
    }

    @PostMapping
    public ResponseEntity post(@RequestBody AddResourceToTopicCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {

            Topic topic = Topic.getById(command.topicid.toString(), graph);
            Resource resource = Resource.getById(command.resourceid.toString(), graph);

            Iterator<Resource> resources = topic.getResources();
            while (resources.hasNext()) {
                Resource r = resources.next();
                if (r.getId().equals(resource.getId()))
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("Topic with id " + command.topicid + " already contains resource with id " + command.resourceid);
            }

            TopicResource topicResource = topic.addResource(resource);
            topicResource.setPrimary(command.primary);

            URI location = URI.create("/topic-resources/" + topicResource.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicResource topicResource = TopicResource.getById(id, graph);
            topicResource.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> put(@PathVariable("id") String id, @RequestBody UpdateTopicResourceCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicResource topicResource = TopicResource.getById(id, graph);
            topicResource.setPrimary(command.primary);
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    public static class AddResourceToTopicCommand {
        @JsonProperty
        public URI topicid, resourceid;

        @JsonProperty
        public boolean primary;
    }

    public static class UpdateTopicResourceCommand {
        @JsonProperty
        public URI id;

        @JsonProperty
        public boolean primary;
    }

    public static class TopicResourceIndexDocument {
        @JsonProperty
        public URI topicid, resourceid, id;

        @JsonProperty
        public boolean primary;

        TopicResourceIndexDocument() {
        }

        TopicResourceIndexDocument(TopicResource topicResource) {
            id = topicResource.getId();
            topicid = topicResource.getTopic().getId();
            resourceid = topicResource.getResource().getId();
            primary = topicResource.isPrimary();
        }
    }
}
