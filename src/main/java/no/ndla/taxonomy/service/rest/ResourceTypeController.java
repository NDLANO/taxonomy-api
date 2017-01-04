package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.ResourceType;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "resource-types")
public class ResourceTypeController {

    private GraphFactory factory;

    public ResourceTypeController(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    public List<ResourceTypeController.ResourceTypeIndexDocument> index() throws Exception {
        List<ResourceTypeController.ResourceTypeIndexDocument> result = new ArrayList<>();

        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, name from `V_Resource-Type`");
            resultSet.iterator().forEachRemaining(record -> {
                ResourceTypeController.ResourceTypeIndexDocument document = new ResourceTypeController.ResourceTypeIndexDocument();
                result.add(document);
                document.id = URI.create(record.field("id"));
                document.name = record.field("name");
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    public ResourceTypeIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            final ResourceType result = ResourceType.getById(id, graph);
            ResourceTypeController.ResourceTypeIndexDocument resourceType = new ResourceTypeController.ResourceTypeIndexDocument(result);
            transaction.rollback();
            return resourceType;
        }
    }

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody CreateResourceTypeCommand command) throws Exception {
        try (Graph graph = factory.create();
        Transaction transaction = graph.tx()) {
            ResourceType resourceType = new ResourceType(graph);
            if (null != command.id) resourceType.setId(command.id.toString());
            resourceType.name(command.name);
            URI location = URI.create("/resource-types/" + resourceType.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        } catch (ORecordDuplicatedException e) {
            throw new DuplicateIdException("" + command.id);
        }
    }

    static class ResourceTypeIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        ResourceTypeIndexDocument() {
        }

        ResourceTypeIndexDocument(ResourceType resourceType) {
            id = resourceType.getId();
            name = resourceType.getName();
        }
    }

    public static class CreateResourceTypeCommand {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;
    }
}
