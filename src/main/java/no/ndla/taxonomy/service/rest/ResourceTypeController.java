package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.ResourceType;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
