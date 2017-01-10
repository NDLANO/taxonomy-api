package no.ndla.taxonomy.service.domain;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.UUID;

public class ResourceResourceType extends DomainEdge{

    public static final String LABEL = "resource-has-resourcetypes";

    /**
     * Creates a new edge between a resource and a resource type
     * @param resource
     * @param resourceType
     */
    public ResourceResourceType(Resource resource, ResourceType resourceType) {
        this(createEdge(resource, resourceType));
        setId("urn:resource-resourcetype:" + UUID.randomUUID());
    }

    /**
     * Wraps an existing edge between a resource and a resource type
     */
    public ResourceResourceType(Edge edge) {
        super(edge);
    }


    private static Edge createEdge(Resource resource, ResourceType resourceType) {
        return resource.vertex.addEdge(ResourceResourceType.LABEL, resourceType.vertex);
    }


    public static ResourceResourceType getById(String id, Graph graph) {
        GraphTraversal<Edge, Edge> traversal = graph.traversal().E().hasLabel(LABEL).has("id", id);
        if (traversal.hasNext()) return new ResourceResourceType((traversal.next()));
        throw new NotFoundException("resource-resourcetype", id);
    }
}
