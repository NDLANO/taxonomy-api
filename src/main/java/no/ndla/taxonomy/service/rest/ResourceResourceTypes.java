package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.ResourceResourceType;
import no.ndla.taxonomy.service.domain.ResourceType;
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
@RequestMapping(path = "resource-resourcetypes")
public class ResourceResourceTypes {

    private GraphFactory factory;

    public ResourceResourceTypes(GraphFactory factory) {
        this.factory = factory;
    }

    @PostMapping
    public ResponseEntity post(@RequestBody CreateResourceResourceTypeCommand command) throws Exception {

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {

            Resource resource = Resource.getById(command.resourceId.toString(), graph);
            ResourceType resourceType = ResourceType.getById(command.resourceTypeId.toString(), graph);

            Iterator<ResourceType> resourceTypes = resource.getResourceTypes();
            while (resourceTypes.hasNext()) {
                ResourceType type = resourceTypes.next();
                if (type.getId().equals(command.resourceTypeId)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("Resource with id " + command.resourceId + " already contains resource type " + command.resourceTypeId);
                }
            }

            ResourceResourceType edge = resource.addResourceType(resourceType);

            URI location = URI.create("/resource-resourcetypes/" + edge.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    @DeleteMapping({"/{id}"})
    public ResponseEntity<Void> delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            ResourceResourceType resourceResourceType = ResourceResourceType.getById(id, graph);
            resourceResourceType.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping
    public List<ResourceResourceTypeIndexDocument> get() throws Exception {

        List<ResourceResourceTypeIndexDocument> result = new ArrayList<>();
        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, out.id as resourceid, in.id as resourcetypeid from `E_resource-has-resourcetypes`");
            resultSet.iterator().forEachRemaining(record -> {
                ResourceResourceTypeIndexDocument document = new ResourceResourceTypes.ResourceResourceTypeIndexDocument();
                document.resourceId = URI.create(record.field("resourceid"));
                document.resourceTypeId = URI.create(record.field("resourcetypeid"));
                document.id = URI.create(record.field("id"));
                result.add(document);
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping({"/{id}"})
    public ResourceResourceTypeIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            final ResourceResourceType result = ResourceResourceType.getById(id, graph);
            final ResourceResourceTypeIndexDocument resourceResourceTypeIndexDocument = new ResourceResourceTypeIndexDocument(result);
            transaction.rollback();
            return new ResourceResourceTypeIndexDocument(result);
        }
    }

    public static class CreateResourceResourceTypeCommand {
        @JsonProperty
        URI resourceId, resourceTypeId;
    }

    public static class ResourceResourceTypeIndexDocument {
        @JsonProperty
        URI resourceId, resourceTypeId, id;

        public ResourceResourceTypeIndexDocument() {}

        public ResourceResourceTypeIndexDocument(ResourceResourceType resourceResourceType) {
            id = resourceResourceType.getId();
            resourceId = resourceResourceType.getResource().getId();
            resourceTypeId = resourceResourceType.getResourceType().getId();
        }
    }
}
