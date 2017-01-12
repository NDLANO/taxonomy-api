package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.GraphFactory;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Resource;
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
@RequestMapping(path = {"resources", "/v1/resources"})
public class Resources {

    private GraphFactory factory;

    public Resources(GraphFactory factory) {
        this.factory = factory;
    }

    @GetMapping
    @ApiOperation(value = "Lists all resources",
            notes = "Multiple status values can be provided with comma seperated strings")
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
    @ApiOperation(value = "Gets a single resource")
    public ResourceIndexDocument get(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = Resource.getById(id, graph);
            ResourceIndexDocument result = new ResourceIndexDocument(resource);
            transaction.rollback();
            return result;
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity delete(@PathVariable("id") String id) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = Resource.getById(id, graph);
            resource.remove();
            transaction.commit();
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(@PathVariable("id") String id, @ApiParam(name= "resource", value = "the updated resource") @RequestBody UpdateResourceCommand command) throws Exception {
        try (Graph graph = factory.create(); Transaction transaction = graph.tx()) {
            Resource resource = Resource.getById(id, graph);
            resource.setName(command.name);
            transaction.commit();
        }
    }

    @PostMapping
    @ApiOperation(value = "Adds a new resource")
    public ResponseEntity<Void> post(
            @ApiParam(name = "resource", value = "the new resource") @RequestBody CreateResourceCommand command) throws Exception {
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
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:resource: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:resource:2")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the resource", example = "Introduction to integration")
        public String name;
    }

    static class UpdateResourceCommand {
        @JsonProperty
        @ApiModelProperty(value = "The name of the resource", example = "Introduction to integration")
        public String name;
    }

    static class ResourceIndexDocument {
        @JsonProperty
        @ApiModelProperty(example="urn:resource:345")
        public URI id;

        @JsonProperty
        @ApiModelProperty( value = "The name of the resource", example = "Introduction to integration")
        public String name;

        ResourceIndexDocument() {
        }

        ResourceIndexDocument(Resource resource) {
            id = resource.getId();
            name = resource.getName();
        }
    }
}
