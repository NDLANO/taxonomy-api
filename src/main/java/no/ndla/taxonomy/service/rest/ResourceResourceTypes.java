package no.ndla.taxonomy.service.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.ResourceResourceType;
import no.ndla.taxonomy.service.domain.ResourceType;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Iterator;

@RestController
@RequestMapping(path = "resource-resourcetypes")
public class ResourceResourceTypes {

    private GraphFactory factory;

    public ResourceResourceTypes(GraphFactory factory) {
        this.factory = factory;
    }

    @PostMapping
    public ResponseEntity createResourceResourceType(@RequestBody CreateResourceResourceTypeCommand command) throws Exception {

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
    public ResponseEntity<Void> deleteResourceResourceType(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            ResourceResourceType resourceResourceType = ResourceResourceType.getById(id, graph);
            resourceResourceType.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    public static class CreateResourceResourceTypeCommand {
        @JsonProperty
        URI resourceId, resourceTypeId;
    }
}
