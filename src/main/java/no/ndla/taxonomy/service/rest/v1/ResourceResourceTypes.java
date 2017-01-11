package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
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
@RequestMapping(path = {"resource-resourcetypes", "/v1/resource-resourcetypes"})
public class ResourceResourceTypes {

    private GraphFactory factory;

    public ResourceResourceTypes(GraphFactory factory) {
        this.factory = factory;
    }

    @PostMapping
    @ApiOperation(value = "Creates a connection between a resource and a resource type")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> post(
            @ApiParam(name = "Connection", value = "The new resource/resource type connection") @RequestBody CreateResourceResourceTypeCommand command) throws Exception {

        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {

            Resource resource = Resource.getById(command.resourceId.toString(), graph);
            ResourceType resourceType = ResourceType.getById(command.resourceTypeId.toString(), graph);

            Iterator<ResourceType> resourceTypes = resource.getResourceTypes();
            while (resourceTypes.hasNext()) {
                ResourceType type = resourceTypes.next();
                if (type.getId().equals(command.resourceTypeId)) {
                    throw new DuplicateIdException(command.resourceTypeId.toString());
                }
            }

            ResourceResourceType edge = resource.addResourceType(resourceType);

            URI location = URI.create("/resource-resourcetypes/" + edge.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        }
    }

    @DeleteMapping({"/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Deletes a connection between a resource and a resource type")
    public void delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            ResourceResourceType resourceResourceType = ResourceResourceType.getById(id, graph);
            resourceResourceType.remove();
            transaction.commit();
        }
    }

    @GetMapping
    @ApiOperation("Gets all connections between resources and resource types")
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
    @ApiOperation("Gets a single connection between resource and resource type")
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
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:123")
        URI resourceId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resource-type:234")
        URI resourceTypeId;
    }

    public static class ResourceResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resource:123")
        URI resourceId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resource-type:234")
        URI resourceTypeId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource to resource type connection id", example = "urn:resource-has-resourcetypes:12")
        URI id;

        public ResourceResourceTypeIndexDocument() {
        }

        public ResourceResourceTypeIndexDocument(ResourceResourceType resourceResourceType) {
            id = resourceResourceType.getId();
            resourceId = resourceResourceType.getResource().getId();
            resourceTypeId = resourceResourceType.getResourceType().getId();
        }
    }
}
