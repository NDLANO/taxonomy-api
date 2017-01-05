package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Resource;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "resources")
public class Resources {

    private GraphFactory factory;

    public Resources(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    public List<ResourceIndexDocument> index() throws Exception {
        List<ResourceIndexDocument> result = new ArrayList<>();

        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, name from V_Resource");
            resultSet.iterator().forEachRemaining(record -> {
                ResourceIndexDocument document = new ResourceIndexDocument();
                result.add(document);
                document.id = URI.create(record.field("id"));
                document.name = record.field("name");
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    public ResourceIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = Resource.getById(id, graph);
            ResourceIndexDocument result = new ResourceIndexDocument(resource);
            transaction.rollback();
            return result;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = Resource.getById(id, graph);
            resource.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity put(@PathVariable("id") String id, @RequestBody UpdateResourceCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = Resource.getById(id, graph);
            resource.setName(command.name);
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody CreateResourceCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = new Resource(graph);
            if (null != command.id) resource.setId(command.id.toString());
            resource.name(command.name);
            URI location = URI.create("/resources/" + resource.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        } catch (ORecordDuplicatedException e) {
            throw new DuplicateIdException("" + command.id);
        }
    }

    public static class CreateResourceCommand {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;
    }

    static class UpdateResourceCommand {
        @JsonProperty
        public String name;
    }

    static class ResourceIndexDocument {
        @JsonProperty
        public URI id;

        @JsonProperty
        public String name;

        ResourceIndexDocument() {
        }

        ResourceIndexDocument(Resource resource) {
            id = resource.getId();
            name = resource.getName();
        }
    }
}
