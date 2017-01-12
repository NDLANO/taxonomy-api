package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
@RequestMapping(path = {"topic-resources", "/v1/topic-resources"})
public class TopicResources {
    private GraphFactory factory;

    public TopicResources(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    @ApiOperation(value = "Gets all connections between topics and resources")
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
    @ApiOperation(value = "Gets a specific connection between a topic and a resource")
    public TopicResourceIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicResource topicResource = TopicResource.getById(id, graph);
            TopicResourceIndexDocument result = new TopicResourceIndexDocument(topicResource);
            transaction.rollback();
            return result;
        }
    }

    @PostMapping
    @ApiOperation(value = "Adds a resource to a topic")
    public ResponseEntity post(
            @ApiParam(name = "connection", value = "new topic/resource connection ") @RequestBody AddResourceToTopicCommand command) throws Exception {
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
    @ApiOperation("Removes a resource from a topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicResource topicResource = TopicResource.getById(id, graph);
            topicResource.remove();
            transaction.commit();
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a connection between a topic and a resource", notes = "Use to update which topic is primary to the resource.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(@PathVariable("id") String id,
                    @ApiParam(name = "connection", value = "Updated topic/resource connection") @RequestBody UpdateTopicResourceCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            TopicResource topicResource = TopicResource.getById(id, graph);
            topicResource.setPrimary(command.primary);
            transaction.commit();
        }
    }

    public static class AddResourceToTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example="urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:345")
        URI resourceid;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example= "true")
        public boolean primary;
    }

    public static class UpdateTopicResourceCommand {
        @JsonProperty
        @ApiModelProperty(value = "Topic resource connection id", example="urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example= "true")
        public boolean primary;
    }

    public static class TopicResourceIndexDocument {

        @JsonProperty
        @ApiModelProperty(value = "Topic id", example="urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Resource id", example = "urn:resource:345")
        URI resourceid;

        @JsonProperty
        @ApiModelProperty(value = "Topic resource connection id", example="urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example= "true")
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
