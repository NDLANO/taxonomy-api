package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.ResourceType;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"resource-types", "/v1/resource-types"})
public class ResourceTypes {

    private GraphFactory factory;

    public ResourceTypes(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    @ApiOperation("Gets a list of all resource types")
    public List<ResourceTypes.ResourceTypeIndexDocument> index() throws Exception {
        List<ResourceTypes.ResourceTypeIndexDocument> result = new ArrayList<>();

        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            Iterable<ODocument> resultSet = (Iterable<ODocument>) graph.executeSql("select id, name from `V_Resource-Type`");
            resultSet.iterator().forEachRemaining(record -> {
                ResourceTypes.ResourceTypeIndexDocument document = new ResourceTypes.ResourceTypeIndexDocument();
                result.add(document);
                document.id = URI.create(record.field("id"));
                document.name = record.field("name");
            });
            transaction.rollback();
            return result;
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single resource type")
    public ResourceTypeIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (OrientGraph graph = (OrientGraph) factory.create(); Transaction transaction = graph.tx()) {
            final ResourceType result = ResourceType.getById(id, graph);
            ResourceTypes.ResourceTypeIndexDocument resourceType = new ResourceTypes.ResourceTypeIndexDocument(result);
            transaction.rollback();
            return resourceType;
        }
    }

    @PostMapping
    @ApiOperation(value = "Adds a new resource type")
    public ResponseEntity<Void> post(
            @ApiParam(name = "resourceType", value = "The new resource type")
            @RequestBody
                    CreateResourceTypeCommand command
    ) throws Exception {
        try (Graph graph = factory.create();
             Transaction transaction = graph.tx()) {
            ResourceType resourceType = new ResourceType(graph);
            if (null != command.id) resourceType.setId(command.id.toString());
            if (null != command.parentId) {
                ResourceType parent = ResourceType.getById(command.parentId.toString(), graph);
                resourceType.addParentResourceType(parent);
            }
            resourceType.name(command.name);
            URI location = URI.create("/resource-types/" + resourceType.getId());
            transaction.commit();
            return ResponseEntity.created(location).build();
        } catch (ORecordDuplicatedException e) {
            throw new DuplicateIdException("" + command.id);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Deletes a single resource type")
    public void delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            ResourceType resource = ResourceType.getById(id, graph);
            resource.remove();
            transaction.commit();
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource type")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable String id,
            @ApiParam(name = "resourceType", value = "The updated resource type") @RequestBody UpdateResourceTypeCommand command
    ) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            ResourceType resourceType = ResourceType.getById(id, graph);
            resourceType.name(command.name);
            transaction.commit();
        }
    }

    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:resource-type:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource type", example = "Lecture")
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
        @ApiModelProperty(value = "If specified, the new resource type will be a child of the mentioned resource type.")
        public URI parentId;

        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:resource-type: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:resource-type:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource type", example = "Lecture")
        public String name;
    }

    public static class UpdateResourceTypeCommand {

        @JsonProperty
        @ApiModelProperty(value = "If specified, the parent of the resource type will be updated")
        public URI parentId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource type", example = "Lecture")
        public String name;
    }
}
